package brenner.edu.reservas.core.domain.exceptions;

/** O check-in foi tentado antes da janela que se abre 10 minutos antes do início. */
public class CheckInAntecipadoException extends DomainException {

    public CheckInAntecipadoException(String message) {
        super(message);
    }
}
