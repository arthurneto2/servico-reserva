package brenner.edu.reservas.core.port;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.shared.Result;

/**
 * Port de saída que carrega uma {@link Reserva} pelo seu identificador.
 *
 * <p>O core declara este contrato; quem o cumpre é um adapter no pacote {@code output}.
 *
 * @see Result
 */
public interface GetReservaPort {

    /**
     * Busca a reserva de identificador {@code id}, qualquer que seja o seu status.
     *
     * <p>Os três estados possíveis do {@link Result} devolvido:
     * <ul>
     *   <li><b>sucesso com valor</b> — a reserva existe; {@code Result.of(reserva)};</li>
     *   <li><b>sucesso vazio</b> — a busca funcionou e não existe reserva com este id;
     *       {@code Result.empty()};</li>
     *   <li><b>falha</b> — problema técnico (banco fora, timeout, erro de mapeamento, invariante
     *       de domínio violado pelo registro persistido); {@code Result.failure(e)}.</li>
     * </ul>
     *
     * <p>Filtrar por status é responsabilidade de quem chama: cancelamento e check-in precisam
     * receber a reserva mesmo já cancelada ou expirada, porque é o domínio que decide se a
     * transição é permitida.
     *
     * <p>Consumo canônico no UseCase:
     * <pre>{@code
     * final var reserva = getReservaPort.getById(reservaId)
     *         .getAvailableValueOrElseThrow(GetReservaUnavailableException::new)
     *         .orElseThrow(() -> new ReservaNotFoundException(reservaId));
     * }</pre>
     *
     * @param id identificador da reserva procurada
     * @return {@code Result} com a reserva, vazio se ela não existe, ou falha se a busca não pôde
     *         ser concluída — nunca {@code null}
     */
    Result<Reserva> getById(ReservaId id);
}
