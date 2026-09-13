package brenner.edu.reservas.core.port;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.shared.Result;

/**
 * Port de saída que persiste uma {@link Reserva}.
 *
 * <p>Um único método serve para criar e para atualizar: a identidade da reserva já existe antes
 * da persistência ({@code ReservaId.novo()} é gerado no domínio), então o adapter faz upsert
 * pelo id e o core não precisa distinguir insert de update.
 *
 * @see Result
 */
public interface SaveReservaPort {

    /**
     * Grava a reserva, criando-a se o id ainda não existe e sobrescrevendo-a se já existe.
     *
     * <p>Os três estados possíveis do {@link Result} devolvido:
     * <ul>
     *   <li><b>sucesso com valor</b> — {@code Result.of(reserva)} com o estado efetivamente
     *       gravado, que é o que o UseCase devolve ao controller;</li>
     *   <li><b>sucesso vazio</b> — não é usado por este port: uma gravação que funcionou sempre
     *       tem um valor a devolver;</li>
     *   <li><b>falha</b> — problema técnico (banco fora, violação de constraint, erro de
     *       mapeamento); {@code Result.failure(e)}.</li>
     * </ul>
     *
     * <p>Consumo canônico no UseCase:
     * <pre>{@code
     * final var salva = saveReservaPort.save(reserva)
     *         .getAvailableValueOrElseThrow(SaveReservaUnavailableException::new)
     *         .orElseThrow(() -> new SaveReservaUnavailableException(
     *                 new IllegalStateException("save devolveu vazio")));
     * }</pre>
     *
     * @param reserva reserva a gravar
     * @return {@code Result} com a reserva gravada, ou falha se a gravação não pôde ser
     *         concluída — nunca {@code null}
     */
    Result<Reserva> save(Reserva reserva);
}
