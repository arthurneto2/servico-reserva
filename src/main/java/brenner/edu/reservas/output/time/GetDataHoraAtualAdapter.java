package brenner.edu.reservas.output.time;

import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.shared.Result;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Adapter de {@link GetDataHoraAtualPort}: o <b>único</b> lugar do projeto autorizado a ler o
 * relógio do sistema.
 *
 * <p>O {@link Clock} é injetável para que o próprio adapter seja testável com o tempo congelado.
 * Isso não devolve o relógio ao core: quem pede a hora continua pedindo por um port e recebendo
 * um {@link Result}, porque um relógio é uma dependência externa como outra qualquer — pode ser
 * o do sistema hoje e um serviço de tempo amanhã.
 *
 * <p>Este port não usa o estado vazio: o relógio ou responde, ou falha.
 */
@Component
public class GetDataHoraAtualAdapter implements GetDataHoraAtualPort {

    private final Clock clock;

    public GetDataHoraAtualAdapter() {
        this(Clock.systemUTC());
    }

    GetDataHoraAtualAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Result<DataHora> get(FusoHorario fuso) {
        try {
            return Result.of(new DataHora(clock.instant(), fuso));
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
}
