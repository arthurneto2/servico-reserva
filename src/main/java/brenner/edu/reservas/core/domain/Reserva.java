package brenner.edu.reservas.core.domain;

import brenner.edu.reservas.core.domain.exceptions.CheckInAntecipadoException;
import brenner.edu.reservas.core.domain.exceptions.PrazoDeCancelamentoExpiradoException;
import brenner.edu.reservas.core.domain.exceptions.ReservaExpiradaException;
import brenner.edu.reservas.core.domain.exceptions.ReservaInvalidaException;
import brenner.edu.reservas.core.domain.exceptions.SolicitanteNaoEOrganizadorException;
import brenner.edu.reservas.core.domain.exceptions.TransicaoDeStatusInvalidaException;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Agregado de reserva. Nasce sempre {@link StatusReserva#PENDENTE} através de
 * {@link #solicitar}; {@link #reconstituir} existe para o adapter de persistência
 * remontar uma reserva já gravada, com o status que ela tiver.
 */
public final class Reserva {

    private static final Set<StatusReserva> STATUS_QUE_OCUPAM_AGENDA =
            EnumSet.of(StatusReserva.PENDENTE, StatusReserva.CONFIRMADA);
    private static final Duration PRAZO_DE_CANCELAMENTO = Duration.ofHours(2);
    private static final Duration ABERTURA_DO_CHECK_IN = Duration.ofMinutes(10);
    private static final Duration TOLERANCIA_DO_CHECK_IN = Duration.ofMinutes(15);

    private final ReservaId id;
    private final SalaId salaId;
    private final Email organizador;
    private final Periodo periodo;
    private final QuantidadeParticipantes participantes;
    private StatusReserva status;
    private final DataHora criadaEm;

    private Reserva(ReservaId id,
                    SalaId salaId,
                    Email organizador,
                    Periodo periodo,
                    QuantidadeParticipantes participantes,
                    StatusReserva status,
                    DataHora criadaEm) {
        if (id == null)
            throw new ReservaInvalidaException("Identificador da reserva não pode ser nulo");
        if (salaId == null)
            throw new ReservaInvalidaException("Identificador da sala não pode ser nulo");
        if (organizador == null)
            throw new ReservaInvalidaException("Organizador não pode ser nulo");
        if (periodo == null)
            throw new ReservaInvalidaException("Período não pode ser nulo");
        if (participantes == null)
            throw new ReservaInvalidaException("Quantidade de participantes não pode ser nula");
        if (status == null)
            throw new ReservaInvalidaException("Status não pode ser nulo");
        if (criadaEm == null)
            throw new ReservaInvalidaException("Data de criação não pode ser nula");

        this.id = id;
        this.salaId = salaId;
        this.organizador = organizador;
        this.periodo = periodo;
        this.participantes = participantes;
        this.status = status;
        this.criadaEm = criadaEm;
    }

    public static Reserva solicitar(ReservaId id,
                                    SalaId salaId,
                                    Email organizador,
                                    Periodo periodo,
                                    QuantidadeParticipantes participantes,
                                    DataHora criadaEm) {
        return new Reserva(id, salaId, organizador, periodo, participantes,
                StatusReserva.PENDENTE, criadaEm);
    }

    public static Reserva reconstituir(ReservaId id,
                                       SalaId salaId,
                                       Email organizador,
                                       Periodo periodo,
                                       QuantidadeParticipantes participantes,
                                       StatusReserva status,
                                       DataHora criadaEm) {
        return new Reserva(id, salaId, organizador, periodo, participantes, status, criadaEm);
    }

    /**
     * Cancela a reserva a pedido de quem a organizou. As regras são avaliadas nesta ordem:
     * status que admite cancelamento, identidade do solicitante e prazo de duas horas antes
     * do início.
     *
     * <p>A ordem importa: uma reserva já cancelada responde "não há o que cancelar" mesmo que
     * o solicitante esteja errado, e um estranho ouve "você não organizou isto" antes de saber
     * se o prazo passou — nenhuma das duas respostas vaza informação que a anterior já negou.
     *
     * @param solicitante quem pede o cancelamento; precisa ser o organizador
     * @param agora       instante atual, sempre recebido de fora: o domínio não lê relógio
     */
    public void cancelar(Email solicitante, DataHora agora) {
        if (solicitante == null)
            throw new ReservaInvalidaException("Solicitante não pode ser nulo");
        if (agora == null)
            throw new ReservaInvalidaException("Data e hora atual não pode ser nula");

        if (!ocupaAgenda())
            throw new TransicaoDeStatusInvalidaException(
                    "Reserva " + status + " não pode ser cancelada");

        if (!organizador.equals(solicitante))
            throw new SolicitanteNaoEOrganizadorException(
                    "Apenas o organizador pode cancelar a reserva " + id.value());

        Duration antecedencia = Duration.between(agora.instante(), periodo.inicio().instante());
        if (antecedencia.compareTo(PRAZO_DE_CANCELAMENTO) < 0)
            throw new PrazoDeCancelamentoExpiradoException(
                    "Cancelamento exige ao menos 2 horas de antecedência; faltam "
                            + antecedencia.toMinutes() + " minutos para o início");

        this.status = StatusReserva.CANCELADA;
    }

    /**
     * Registra a presença do organizador, confirmando a reserva. A janela de check-in vai de
     * 10 minutos antes do início até 15 minutos depois dele, ambos os limites inclusivos.
     *
     * <p>Fora da janela as duas pontas são assimétricas: chegar cedo demais é só uma recusa —
     * a reserva segue PENDENTE e o organizador pode voltar —, enquanto chegar tarde demais
     * <b>expira</b> a reserva antes de recusar. A reserva expirada libera a agenda, e por isso
     * o novo estado precisa ser gravado mesmo tendo vindo junto de uma exceção; quem chama é
     * responsável por persistir antes de propagar.
     *
     * @param agora instante atual, sempre recebido de fora: o domínio não lê relógio
     * @throws ReservaExpiradaException depois da janela — e, antes de lançar, muda o status
     *                                  para {@link StatusReserva#EXPIRADA}
     */
    public void realizarCheckIn(DataHora agora) {
        if (agora == null)
            throw new ReservaInvalidaException("Data e hora atual não pode ser nula");

        if (status != StatusReserva.PENDENTE)
            throw new TransicaoDeStatusInvalidaException(
                    "Check-in exige reserva PENDENTE; reserva está " + status);

        DataHora aberturaDoCheckIn = periodo.inicio().menos(ABERTURA_DO_CHECK_IN);
        if (agora.antesDe(aberturaDoCheckIn))
            throw new CheckInAntecipadoException(
                    "Check-in só é liberado a partir de " + emMinutos(aberturaDoCheckIn)
                            + "; agora são " + emMinutos(agora));

        DataHora limiteDoCheckIn = periodo.inicio().mais(TOLERANCIA_DO_CHECK_IN);
        if (agora.depoisDe(limiteDoCheckIn)) {
            this.status = StatusReserva.EXPIRADA;
            throw new ReservaExpiradaException(
                    "Check-in encerrou às " + emMinutos(limiteDoCheckIn) + "; reserva "
                            + id.value() + " expirou");
        }

        this.status = StatusReserva.CONFIRMADA;
    }

    /** Hora local sem segundos: as mensagens falam de horário de agenda, não de cronômetro. */
    private static LocalTime emMinutos(DataHora dataHora) {
        return dataHora.hora().truncatedTo(ChronoUnit.MINUTES);
    }

    /** Uma reserva só bloqueia a agenda enquanto está PENDENTE ou CONFIRMADA. */
    public boolean ocupaAgenda() {
        return STATUS_QUE_OCUPAM_AGENDA.contains(status);
    }

    public ReservaId id() {
        return id;
    }

    public SalaId salaId() {
        return salaId;
    }

    public Email organizador() {
        return organizador;
    }

    public Periodo periodo() {
        return periodo;
    }

    public QuantidadeParticipantes participantes() {
        return participantes;
    }

    public StatusReserva status() {
        return status;
    }

    public DataHora criadaEm() {
        return criadaEm;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Reserva reserva && id.equals(reserva.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reserva[id=" + id.value() + ", sala=" + salaId.value() + ", status=" + status + "]";
    }
}
