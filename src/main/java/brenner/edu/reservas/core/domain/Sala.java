package brenner.edu.reservas.core.domain;

import brenner.edu.reservas.core.domain.exceptions.AntecedenciaMinimaException;
import brenner.edu.reservas.core.domain.exceptions.CapacidadeExcedidaException;
import brenner.edu.reservas.core.domain.exceptions.ConflitoDeHorarioException;
import brenner.edu.reservas.core.domain.exceptions.FusoIncompativelException;
import brenner.edu.reservas.core.domain.exceptions.SalaInativaException;
import brenner.edu.reservas.core.domain.exceptions.SalaInvalidaException;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.JanelaLivre;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class Sala {

    private static final LocalTime HORARIO_ABERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORARIO_FECHAMENTO = LocalTime.of(20, 0);
    private static final Duration ANTECEDENCIA_MINIMA = Duration.ofHours(1);

    private final SalaId id;
    private final String nome;
    private final Capacidade capacidade;
    private final FusoHorario fuso;
    private final boolean ativa;

    public Sala(SalaId id, String nome, Capacidade capacidade, FusoHorario fuso, boolean ativa) {
        if (id == null)
            throw new SalaInvalidaException("Identificador da sala não pode ser nulo");
        if (nome == null || nome.isBlank())
            throw new SalaInvalidaException("Nome da sala não pode ser vazio");
        if (capacidade == null)
            throw new SalaInvalidaException("Capacidade da sala não pode ser nula");
        if (fuso == null)
            throw new SalaInvalidaException("Fuso horário da sala não pode ser nulo");

        this.id = id;
        this.nome = nome.trim();
        this.capacidade = capacidade;
        this.fuso = fuso;
        this.ativa = ativa;
    }

    /**
     * Solicita uma reserva nesta sala. As regras são avaliadas nesta ordem: sala ativa,
     * fuso do período igual ao da sala, capacidade, antecedência mínima de uma hora e
     * conflito com as reservas já ativas.
     *
     * <p>O fuso é checado porque {@link Periodo} valida a janela 08:00–20:00 no seu próprio
     * fuso: sem essa checagem, um período montado em outro fuso passaria por válido e
     * ocuparia a sala fora do horário de funcionamento dela.
     *
     * @param reservasAtivas reservas que já ocupam a agenda desta sala; a filtragem por
     *                       status é responsabilidade de quem chama
     * @return uma reserva nova, {@link StatusReserva#PENDENTE}, criada em {@code agora}
     */
    public Reserva reservar(Email organizador,
                            Periodo periodo,
                            QuantidadeParticipantes participantes,
                            List<Reserva> reservasAtivas,
                            DataHora agora) {
        if (organizador == null)
            throw new SalaInvalidaException("Organizador não pode ser nulo");
        if (periodo == null)
            throw new SalaInvalidaException("Período não pode ser nulo");
        if (participantes == null)
            throw new SalaInvalidaException("Quantidade de participantes não pode ser nula");
        if (reservasAtivas == null)
            throw new SalaInvalidaException("Lista de reservas ativas não pode ser nula");
        if (agora == null)
            throw new SalaInvalidaException("Data e hora atual não pode ser nula");

        if (!ativa)
            throw new SalaInativaException("Sala inativa não aceita reservas: " + nome);

        if (!periodo.inicio().fusoHorario().equals(fuso))
            throw new FusoIncompativelException(
                    "Período deve estar no fuso horário da sala: sala em " + fuso.value()
                            + ", período em " + periodo.inicio().fusoHorario().value());

        if (participantes.excede(capacidade))
            throw new CapacidadeExcedidaException(
                    "Sala " + nome + " comporta " + capacidade.value()
                            + " participantes, solicitados " + participantes.value());

        Duration antecedencia = Duration.between(agora.instante(), periodo.inicio().instante());
        if (antecedencia.compareTo(ANTECEDENCIA_MINIMA) < 0)
            throw new AntecedenciaMinimaException(
                    "Reserva exige ao menos 1 hora de antecedência; faltam "
                            + antecedencia.toMinutes() + " minutos para o início");

        for (Reserva reservaAtiva : reservasAtivas) {
            if (periodo.sobrepoe(reservaAtiva.periodo()))
                throw new ConflitoDeHorarioException(
                        "Período conflita com a reserva " + reservaAtiva.id().value()
                                + " na sala " + nome);
        }

        return Reserva.solicitar(ReservaId.novo(), id, organizador, periodo, participantes, agora);
    }

    /**
     * Lacunas livres da agenda entre 08:00 e 20:00 no fuso da sala, descontando os períodos
     * das reservas PENDENTE e CONFIRMADA. Reservas com outro status — ou que não tocam o dia
     * pedido — são ignoradas.
     *
     * @return as lacunas em ordem cronológica; lista vazia se o dia estiver todo ocupado
     */
    public List<JanelaLivre> horariosLivresEm(LocalDate data, List<Reserva> reservasDoDia) {
        if (data == null)
            throw new SalaInvalidaException("Data não pode ser nula");
        if (reservasDoDia == null)
            throw new SalaInvalidaException("Lista de reservas do dia não pode ser nula");

        Instant abertura = instanteLocal(data, HORARIO_ABERTURA);
        Instant fechamento = instanteLocal(data, HORARIO_FECHAMENTO);

        List<Periodo> ocupados = reservasDoDia.stream()
                .filter(Reserva::ocupaAgenda)
                .map(Reserva::periodo)
                .filter(periodo -> periodo.inicio().instante().isBefore(fechamento)
                        && periodo.fim().instante().isAfter(abertura))
                .sorted(Comparator.comparing(periodo -> periodo.inicio().instante()))
                .toList();

        List<JanelaLivre> livres = new ArrayList<>();
        Instant cursor = abertura;

        for (Periodo ocupado : ocupados) {
            Instant inicioOcupado = maximo(ocupado.inicio().instante(), abertura);
            Instant fimOcupado = minimo(ocupado.fim().instante(), fechamento);

            if (cursor.isBefore(inicioOcupado))
                livres.add(new JanelaLivre(em(cursor), em(inicioOcupado)));

            if (fimOcupado.isAfter(cursor))
                cursor = fimOcupado;
        }

        if (cursor.isBefore(fechamento))
            livres.add(new JanelaLivre(em(cursor), em(fechamento)));

        return List.copyOf(livres);
    }

    private Instant instanteLocal(LocalDate data, LocalTime hora) {
        return LocalDateTime.of(data, hora).atZone(fuso.value()).toInstant();
    }

    private DataHora em(Instant instante) {
        return new DataHora(instante, fuso);
    }

    private static Instant maximo(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant minimo(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    public SalaId id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public Capacidade capacidade() {
        return capacidade;
    }

    public FusoHorario fuso() {
        return fuso;
    }

    public boolean ativa() {
        return ativa;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof Sala sala && id.equals(sala.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Sala[id=" + id.value() + ", nome=" + nome + ", ativa=" + ativa + "]";
    }
}
