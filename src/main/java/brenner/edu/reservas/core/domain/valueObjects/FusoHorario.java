package brenner.edu.reservas.core.domain.valueObjects;

import brenner.edu.reservas.core.domain.exceptions.FusoHorarioInvalidoException;

import java.time.DateTimeException;
import java.time.ZoneId;

public record FusoHorario(ZoneId value) {

    public FusoHorario{
        if (value == null)
            throw new FusoHorarioInvalidoException("Fuso horário não pode ser nulo");
    }

    public static FusoHorario of(String zone) {
        if(zone == null || zone.isBlank())
            throw new FusoHorarioInvalidoException("Fuso horário não pode ser nulo");

        try {
            return new FusoHorario(ZoneId.of(zone.trim()));
        } catch (DateTimeException e) {
            throw new FusoHorarioInvalidoException("Fuso horário inválido: " + zone, e);
        }
    }
}
