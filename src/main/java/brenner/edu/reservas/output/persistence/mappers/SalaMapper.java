package brenner.edu.reservas.output.persistence.mappers;

import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.output.persistence.entities.SalaEntity;
import org.springframework.stereotype.Component;

/**
 * Converte {@link SalaEntity} ↔ {@link Sala}.
 *
 * <p>Reconstruir o domínio é reexecutar suas invariantes: um fuso inválido ou um nome vazio que
 * tenham entrado no banco por fora da aplicação explodem <b>aqui</b>, e o adapter que chamou
 * este mapper transforma a exceção em {@code Result.failure(e)}.
 */
@Component
public class SalaMapper {

    public Sala toDomain(SalaEntity entity) {
        return new Sala(new SalaId(entity.getId()),
                entity.getNome(),
                new Capacidade(entity.getCapacidade()),
                FusoHorario.of(entity.getFusoHorario()),
                entity.isAtiva());
    }

    public SalaEntity toEntity(Sala sala) {
        SalaEntity entity = new SalaEntity();
        entity.setId(sala.id().value());
        entity.setNome(sala.nome());
        entity.setCapacidade(sala.capacidade().value());
        entity.setFusoHorario(sala.fuso().value().getId());
        entity.setAtiva(sala.ativa());
        return entity;
    }
}
