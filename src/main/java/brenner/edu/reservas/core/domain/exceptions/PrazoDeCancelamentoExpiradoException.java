package brenner.edu.reservas.core.domain.exceptions;

/** O cancelamento foi pedido a menos de duas horas do início da reserva. */
public class PrazoDeCancelamentoExpiradoException extends DomainException {

    public PrazoDeCancelamentoExpiradoException(String message) {
        super(message);
    }
}
