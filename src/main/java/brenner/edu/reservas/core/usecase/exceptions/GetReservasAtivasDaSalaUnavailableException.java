package brenner.edu.reservas.core.usecase.exceptions;

/** A consulta da agenda da sala não pôde ser concluída — 503. A causa vem do adapter, dentro do {@code Result.failure(e)}. */
public class GetReservasAtivasDaSalaUnavailableException extends UnavailableException {

    public GetReservasAtivasDaSalaUnavailableException(Throwable causa) {
        super("A consulta da agenda da sala não pôde ser concluída", causa);
    }
}
