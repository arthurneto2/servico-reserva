package brenner.edu.reservas.core.usecase.exceptions;

/**
 * Base das exceções de "dependência externa não respondeu" — mapeadas para <b>503</b> pelo
 * {@code @ControllerAdvice}.
 *
 * <p>Guarda sempre a causa original: o adapter engoliu a exceção técnica dentro de
 * {@code Result.failure(e)}, e esta é a única forma de o operador descobrir o que realmente
 * aconteceu. O handler é obrigado a logar {@link #getCause()}.
 */
public abstract class UnavailableException extends RuntimeException {

    protected UnavailableException(String message, Throwable causa) {
        super(message, causa);
    }
}
