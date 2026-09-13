package brenner.edu.reservas.core.domain.exceptions;

/** A reserva está em um status que não admite a transição pedida (cancelar, fazer check-in). */
public class TransicaoDeStatusInvalidaException extends DomainException {

    public TransicaoDeStatusInvalidaException(String message) {
        super(message);
    }
}
