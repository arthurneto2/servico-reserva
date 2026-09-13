package brenner.edu.reservas.core.domain.exceptions;

import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;

public class FusoHorarioInvalidoException extends RuntimeException {
    public FusoHorarioInvalidoException(String message) {
        super(message);
    }

    public FusoHorarioInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
