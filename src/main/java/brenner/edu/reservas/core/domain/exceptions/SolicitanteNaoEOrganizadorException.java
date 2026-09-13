package brenner.edu.reservas.core.domain.exceptions;

/** Quem pediu o cancelamento não é quem organizou a reserva. */
public class SolicitanteNaoEOrganizadorException extends DomainException {

    public SolicitanteNaoEOrganizadorException(String message) {
        super(message);
    }
}
