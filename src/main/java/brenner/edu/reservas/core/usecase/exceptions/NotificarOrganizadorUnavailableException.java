package brenner.edu.reservas.core.usecase.exceptions;

/** A notificação do organizador não pôde ser concluída — 503. A causa vem do adapter, dentro do {@code Result.failure(e)}. */
public class NotificarOrganizadorUnavailableException extends UnavailableException {

    public NotificarOrganizadorUnavailableException(Throwable causa) {
        super("A notificação do organizador não pôde ser concluída", causa);
    }
}
