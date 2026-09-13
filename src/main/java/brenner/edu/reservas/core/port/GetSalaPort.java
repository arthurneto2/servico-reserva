package brenner.edu.reservas.core.port;

import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.shared.Result;

/**
 * Port de saída que carrega uma {@link Sala} pelo seu identificador.
 *
 * <p>O core declara este contrato; quem o cumpre é um adapter no pacote {@code output}
 * (tipicamente sobre JPA). O core nunca sabe de onde a sala vem.
 *
 * @see Result
 */
public interface GetSalaPort {

    /**
     * Busca a sala de identificador {@code id}.
     *
     * <p>Os três estados possíveis do {@link Result} devolvido:
     * <ul>
     *   <li><b>sucesso com valor</b> — a sala existe; o adapter devolve {@code Result.of(sala)};</li>
     *   <li><b>sucesso vazio</b> — a busca funcionou e não existe sala com este id;
     *       o adapter devolve {@code Result.empty()};</li>
     *   <li><b>falha</b> — qualquer problema técnico (banco fora, timeout, erro de mapeamento,
     *       invariante de domínio violado pelo registro persistido) vira {@code Result.failure(e)}.</li>
     * </ul>
     *
     * <p>O adapter jamais deixa exceção escapar e jamais declara {@code throws}: captura tudo e
     * encapsula em {@code Result.failure(e)}.
     *
     * <p>Consumo canônico no UseCase — a indisponibilidade vira 503 e a ausência vira 404:
     * <pre>{@code
     * final var sala = getSalaPort.getById(salaId)
     *         .getAvailableValueOrElseThrow(GetSalaUnavailableException::new)
     *         .orElseThrow(() -> new SalaNotFoundException(salaId));
     * }</pre>
     *
     * @param id identificador da sala procurada; um {@code id} nulo é problema do chamador e o
     *           adapter o trata como falha técnica, não como ausência
     * @return {@code Result} com a sala, vazio se ela não existe, ou falha se a busca não pôde
     *         ser concluída — nunca {@code null}
     */
    Result<Sala> getById(SalaId id);
}
