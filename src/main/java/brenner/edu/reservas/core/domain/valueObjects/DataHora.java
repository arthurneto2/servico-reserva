package brenner.edu.reservas.core.domain.valueObjects;

import brenner.edu.reservas.core.domain.exceptions.DataHoraInvalidaException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record DataHora(Instant instante, FusoHorario fusoHorario) {

    public DataHora{
        if (instante == null)
            throw new DataHoraInvalidaException("Data e hora não pode ser nulo");
        if (fusoHorario == null)
            throw new DataHoraInvalidaException("Fuso horário não pode ser nulo");
    }

    public LocalDate data(){
        return instante.atZone(fusoHorario.value()).toLocalDate();
    }

    public LocalTime hora(){
        return instante.atZone(fusoHorario.value()).toLocalTime();
    }

    public DataHora mais(Duration duracao){
        return new DataHora(instante.plus(duracao), fusoHorario);
    }

    public DataHora menos(Duration duracao){
        return new DataHora(instante.minus(duracao), fusoHorario);
    }

    public boolean antesDe(DataHora outraDataHora){
        return instante.isBefore(outraDataHora.instante);
    }

    public boolean depoisDe(DataHora outraDataHora){
        return instante.isAfter(outraDataHora.instante);
    }

    public boolean entre(DataHora dataHoraInicio, DataHora dataHoraFim){
        return instante.isAfter(dataHoraInicio.instante) && instante.isBefore(dataHoraFim.instante);
    }

}
