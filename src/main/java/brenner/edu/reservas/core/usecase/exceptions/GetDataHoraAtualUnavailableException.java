package brenner.edu.reservas.core.usecase.exceptions;

/** A consulta do relógio não pôde ser concluída — 503. A causa vem do adapter, dentro do {@code Result.failure(e)}. */
public class GetDataHoraAtualUnavailableException extends UnavailableException {

    public GetDataHoraAtualUnavailableException(Throwable causa) {
        super("A consulta do relógio não pôde ser concluída", causa);
    }
}
