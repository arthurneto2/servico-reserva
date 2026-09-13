package brenner.edu.reservas.core.domain;

import brenner.edu.reservas.core.domain.exceptions.ReservaInvalidaException;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;

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

    private final ReservaId id;
    private final SalaId salaId;
    private final Email organizador;
    private final Periodo periodo;
    private final QuantidadeParticipantes participantes;
    private final StatusReserva status;
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
