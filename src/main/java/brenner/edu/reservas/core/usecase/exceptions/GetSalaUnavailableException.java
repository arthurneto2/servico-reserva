package brenner.edu.reservas.core.usecase.exceptions;

/** A busca da sala não pôde ser concluída — 503. A causa vem do adapter, dentro do {@code Result.failure(e)}. */
public class GetSalaUnavailableException extends UnavailableException {

    public GetSalaUnavailableException(Throwable causa) {
        super("A busca da sala não pôde ser concluída", causa);
    }
}
