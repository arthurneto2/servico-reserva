package brenner.edu.reservas.output.persistence.adapters;

import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.output.persistence.SalaRepository;
import brenner.edu.reservas.output.persistence.mappers.SalaMapper;
import brenner.edu.reservas.shared.Result;
import org.springframework.stereotype.Component;

/**
 * Adapter JPA de {@link GetSalaPort}.
 *
 * <p>Os três estados do {@link Result} aparecem aqui inteiros: sala encontrada, sala inexistente
 * e problema técnico. O {@code catch (Exception)} é largo de propósito — inclui tanto a falha do
 * banco quanto a invariante de domínio violada por um registro gravado por fora da aplicação
 * (um fuso que não existe mais, por exemplo). Para quem chama, ambos significam a mesma coisa:
 * não deu para obter a sala agora.
 */
@Component
public class GetSalaAdapter implements GetSalaPort {

    private final SalaRepository repository;
    private final SalaMapper mapper;

    public GetSalaAdapter(SalaRepository repository, SalaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Result<Sala> getById(SalaId id) {
        try {
            return repository.findById(id.value())
                    .map(mapper::toDomain)
                    .map(Result::of)
                    .orElseGet(Result::empty);
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
}
