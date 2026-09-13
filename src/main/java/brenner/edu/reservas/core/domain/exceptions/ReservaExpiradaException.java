package brenner.edu.reservas.core.domain.exceptions;

/**
 * O check-in foi tentado depois da janela que se fecha 15 minutos após o início — a reserva
 * expirou.
 *
 * <p>É a única exceção do domínio lançada <b>junto</b> com uma mudança de estado: a reserva já
 * está EXPIRADA quando ela chega ao UseCase, e precisa ser gravada assim. Por isso o
 * {@code RealizarCheckInUseCase} grava antes de propagar, com
 * {@code @Transactional(noRollbackFor = ReservaExpiradaException.class)}.
 */
public class ReservaExpiradaException extends DomainException {

    public ReservaExpiradaException(String message) {
        super(message);
    }
}
