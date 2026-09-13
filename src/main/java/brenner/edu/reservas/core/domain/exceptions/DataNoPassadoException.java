package brenner.edu.reservas.core.domain.exceptions;

/**
 * A data consultada já passou no fuso da sala.
 *
 * <p>Mora no domínio, e não em {@code core.usecase}, porque é uma regra de negócio — a agenda de
 * ontem não se consulta — e é assim que o handler a mapeia para 422. Quem a lança, porém, é o
 * UseCase: comparar a data pedida com "hoje" exige o agora do fuso da sala, que chega de um port.
 */
public class DataNoPassadoException extends DomainException {

    public DataNoPassadoException(String message) {
        super(message);
    }
}
