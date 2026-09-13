package brenner.edu.reservas.core.usecase.exceptions;

/** A busca da reserva não pôde ser concluída — 503. A causa vem do adapter, dentro do {@code Result.failure(e)}. */
public class GetReservaUnavailableException extends UnavailableException {

    public GetReservaUnavailableException(Throwable causa) {
        super("A busca da reserva não pôde ser concluída", causa);
    }
}
