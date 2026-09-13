package brenner.edu.reservas.core.domain.exceptions;

public class FusoHorarioInvalidoException extends DomainException {
    public FusoHorarioInvalidoException(String message) {
        super(message);
    }

    public FusoHorarioInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
