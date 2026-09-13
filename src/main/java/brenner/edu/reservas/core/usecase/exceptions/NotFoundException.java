package brenner.edu.reservas.core.usecase.exceptions;

/**
 * Base das exceções de "recurso pedido não existe" — mapeadas para <b>404</b> pelo
 * {@code @ControllerAdvice}.
 *
 * <p>Nasce da orquestração, não do domínio: um port devolveu {@code Result.empty()} e o UseCase
 * decidiu que aquela ausência é um erro do chamador. Por isso mora em {@code core.usecase} e não
 * herda de {@code DomainException} — regra de negócio violada é 422, recurso inexistente é 404.
 */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
