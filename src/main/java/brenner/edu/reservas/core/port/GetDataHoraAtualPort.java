package brenner.edu.reservas.core.port;

import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.shared.Result;

/**
 * Port de saída que fornece o instante atual no fuso pedido.
 *
 * <p>O relógio é um port, e não uma chamada direta a {@code Instant.now()}, por dois motivos:
 * o domínio precisa <b>receber</b> o "agora" como parâmetro para ser testável sem congelar o
 * tempo, e cada sala vive em um fuso diferente, então "agora" só tem sentido junto de um
 * {@link FusoHorario}.
 *
 * <p>É o único lugar do projeto autorizado a ler o relógio do sistema — via o adapter em
 * {@code output.time}.
 *
 * @see Result
 */
public interface GetDataHoraAtualPort {

    /**
     * Devolve o instante atual expresso em {@code fuso}.
     *
     * <p>Os estados possíveis do {@link Result} devolvido:
     * <ul>
     *   <li><b>sucesso com valor</b> — {@code Result.of(agora)};</li>
     *   <li><b>sucesso vazio</b> — não é usado por este port: o relógio ou responde, ou falha;</li>
     *   <li><b>falha</b> — o fuso é inválido ou a fonte de tempo não respondeu;
     *       {@code Result.failure(e)}.</li>
     * </ul>
     *
     * <p>Consumo canônico no UseCase — não há 404 possível, então o vazio também vira 503:
     * <pre>{@code
     * final var agora = getDataHoraAtualPort.get(sala.fuso())
     *         .getAvailableValueOrElseThrow(GetDataHoraAtualUnavailableException::new)
     *         .orElseThrow(() -> new GetDataHoraAtualUnavailableException(
     *                 new IllegalStateException("relógio devolveu vazio")));
     * }</pre>
     *
     * @param fuso fuso em que o instante deve ser expresso, tipicamente o da sala envolvida
     * @return {@code Result} com o instante atual, ou falha se ele não pôde ser obtido — nunca
     *         {@code null}
     */
    Result<DataHora> get(FusoHorario fuso);
}
