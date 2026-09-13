package brenner.edu.reservas.output.persistence.adapters;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.port.GetReservaPort;
import brenner.edu.reservas.output.persistence.ReservaRepository;
import brenner.edu.reservas.output.persistence.SalaRepository;
import brenner.edu.reservas.output.persistence.mappers.ReservaMapper;
import brenner.edu.reservas.output.persistence.mappers.SalaMapper;
import brenner.edu.reservas.shared.Result;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter JPA de {@link GetReservaPort}.
 *
 * <p>Carrega também a sala, porque a tabela {@code reserva} guarda instantes e o domínio precisa
 * de {@code DataHora}, que só existe com um fuso — e o fuso é da sala.
 *
 * <p>Se a sala referenciada não existir, o resultado é <b>falha</b>, não ausência: a FK do DDL
 * garante que ela existe, então sua falta significa banco inconsistente (503), e não "a reserva
 * não foi encontrada" (404).
 */
@Component
public class GetReservaAdapter implements GetReservaPort {

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final ReservaMapper reservaMapper;
    private final SalaMapper salaMapper;

    public GetReservaAdapter(ReservaRepository reservaRepository,
                             SalaRepository salaRepository,
                             ReservaMapper reservaMapper,
                             SalaMapper salaMapper) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.reservaMapper = reservaMapper;
        this.salaMapper = salaMapper;
    }

    @Override
    public Result<Reserva> getById(ReservaId id) {
        try {
            return reservaRepository.findById(id.value())
                    .map(entity -> reservaMapper.toDomain(entity, fusoDaSala(entity.getSalaId())))
                    .map(Result::of)
                    .orElseGet(Result::empty);
        } catch (Exception e) {
            return Result.failure(e);
        }
    }

    private brenner.edu.reservas.core.domain.valueObjects.FusoHorario fusoDaSala(UUID salaId) {
        return salaRepository.findById(salaId)
                .map(salaMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "Reserva aponta para a sala inexistente " + salaId))
                .fuso();
    }
}
