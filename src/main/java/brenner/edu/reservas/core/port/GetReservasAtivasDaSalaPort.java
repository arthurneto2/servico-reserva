package brenner.edu.reservas.core.port;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.shared.Result;

import java.util.List;

/**
 * Port de saída que lista as reservas que ocupam a agenda de uma sala em uma janela de tempo.
 *
 * <p>Alimenta tanto {@code Sala.reservar(...)} — que precisa das reservas concorrentes para
 * detectar conflito — quanto {@code Sala.horariosLivresEm(...)}.
 *
 * @see Result
 */
public interface GetReservasAtivasDaSalaPort {

    /**
     * Lista as reservas da sala {@code id} que ocupam a agenda e tocam a janela
     * {@code [inicio, fim)}.
     *
     * <p>"Ativas" significa status {@link StatusReserva#PENDENTE} ou
     * {@link StatusReserva#CONFIRMADA}: o filtro é responsabilidade do adapter, e não de quem
     * chama. Uma reserva "toca" a janela quando começa antes de {@code fim} e termina depois de
     * {@code inicio} — reservas que apenas encostam nas bordas não conflitam.
     *
     * <p>Os três estados possíveis do {@link Result} devolvido:
     * <ul>
     *   <li><b>sucesso com valor</b> — {@code Result.of(lista)}, inclusive quando a lista está
     *       vazia: agenda livre é um resultado, não uma ausência;</li>
     *   <li><b>sucesso vazio</b> — não é usado por este port; agenda livre é
     *       {@code Result.of(List.of())};</li>
     *   <li><b>falha</b> — problema técnico; {@code Result.failure(e)}.</li>
     * </ul>
     *
     * <p>Consumo canônico no UseCase — como o vazio não acontece, o {@code orElseGet} é apenas
     * uma rede de segurança:
     * <pre>{@code
     * final var reservasAtivas = getReservasAtivasDaSalaPort.get(salaId, inicioDoDia, fimDoDia)
     *         .getAvailableValueOrElseThrow(GetReservasAtivasDaSalaUnavailableException::new)
     *         .orElseGet(List::of);
     * }</pre>
     *
     * @param id     identificador da sala cuja agenda será lida
     * @param inicio início da janela, inclusivo
     * @param fim    fim da janela, exclusivo
     * @return {@code Result} com a lista — possivelmente vazia — ou falha se a busca não pôde ser
     *         concluída; nunca {@code null}
     */
    Result<List<Reserva>> get(SalaId id, DataHora inicio, DataHora fim);
}
