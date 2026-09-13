package brenner.edu.reservas.core.port;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.shared.Result;

/**
 * Port de saída que avisa o organizador sobre uma {@link Reserva}.
 *
 * <p>Neste projeto o adapter apenas loga, mas o contrato é o de uma dependência externa de
 * verdade: pode estar fora do ar, e por isso devolve {@link Result}. Como a notificação é
 * chamada dentro da transação do UseCase, uma falha aqui reverte a reserva.
 *
 * @see Result
 */
public interface NotificarOrganizadorPort {

    /**
     * Notifica o organizador da reserva.
     *
     * <p>Este port não tem valor a devolver, logo o {@code Result} usa apenas dois dos seus três
     * estados:
     * <ul>
     *   <li><b>sucesso vazio</b> — a notificação foi enviada; {@code Result.empty()}. Note que
     *       {@code Result.of(null)} é proibido, então o sucesso de um {@code Result<Void>} é
     *       necessariamente o estado vazio;</li>
     *   <li><b>falha</b> — o envio não pôde ser concluído; {@code Result.failure(e)}.</li>
     * </ul>
     *
     * <p>Consumo canônico no UseCase — o {@code Optional} devolvido é ignorado, porque o que
     * importa é a chamada não ter lançado:
     * <pre>{@code
     * notificarOrganizadorPort.notificar(reserva)
     *         .getAvailableValueOrElseThrow(NotificarOrganizadorUnavailableException::new);
     * }</pre>
     *
     * @param reserva reserva sobre a qual notificar; o destinatário é {@code reserva.organizador()}
     * @return {@code Result} vazio em caso de sucesso, ou falha se a notificação não pôde ser
     *         enviada — nunca {@code null}
     */
    Result<Void> notificar(Reserva reserva);
}
