package brenner.edu.reservas.output.persistence.adapters;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.port.SaveReservaPort;
import brenner.edu.reservas.output.persistence.ReservaRepository;
import brenner.edu.reservas.output.persistence.mappers.ReservaMapper;
import brenner.edu.reservas.shared.Result;
import org.springframework.stereotype.Component;

/**
 * Adapter JPA de {@link SaveReservaPort}.
 *
 * <p>Um único método cria e atualiza: o id nasce no domínio ({@code ReservaId.novo()}), então
 * {@code JpaRepository.save} faz upsert por ele e o core não precisa distinguir insert de update.
 *
 * <p>Diferente dos adapters de leitura, aqui não há consulta à sala para descobrir o fuso: ele
 * veio junto da reserva que está sendo gravada.
 */
@Component
public class SaveReservaAdapter implements SaveReservaPort {

    private final ReservaRepository repository;
    private final ReservaMapper mapper;

    public SaveReservaAdapter(ReservaRepository repository, ReservaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Result<Reserva> save(Reserva reserva) {
        try {
            var fuso = reserva.periodo().inicio().fusoHorario();
            var gravada = repository.save(mapper.toEntity(reserva));
            return Result.of(mapper.toDomain(gravada, fuso));
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
}
