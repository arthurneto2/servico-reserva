package brenner.edu.reservas.core.usecase.exceptions;

/** A gravação da reserva não pôde ser concluída — 503. A causa vem do adapter, dentro do {@code Result.failure(e)}. */
public class SaveReservaUnavailableException extends UnavailableException {

    public SaveReservaUnavailableException(Throwable causa) {
        super("A gravação da reserva não pôde ser concluída", causa);
    }
}
