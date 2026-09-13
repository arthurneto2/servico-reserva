package brenner.edu.reservas.output.persistence.mappers;

import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.exceptions.FusoHorarioInvalidoException;
import brenner.edu.reservas.core.domain.exceptions.SalaInvalidaException;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.output.persistence.entities.SalaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaMapperTest {

    private static final UUID SALA_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final SalaMapper mapper = new SalaMapper();

    private static SalaEntity entidade() {
        SalaEntity entity = new SalaEntity();
        entity.setId(SALA_UUID);
        entity.setNome("Sala Pirapora");
        entity.setCapacidade(8);
        entity.setFusoHorario("America/Sao_Paulo");
        entity.setAtiva(true);
        return entity;
    }

    @Nested
    @DisplayName("entidade → domínio")
    class ParaDominio {

        @Test
        void reconstroiTodosOsCampos() {
            Sala sala = mapper.toDomain(entidade());

            assertAll(
                    () -> assertEquals(new SalaId(SALA_UUID), sala.id()),
                    () -> assertEquals("Sala Pirapora", sala.nome()),
                    () -> assertEquals(new Capacidade(8), sala.capacidade()),
                    () -> assertEquals(FusoHorario.of("America/Sao_Paulo"), sala.fuso()),
                    () -> assertTrue(sala.ativa()));
        }

        @Test
        void preservaSalaInativa() {
            SalaEntity entity = entidade();
            entity.setAtiva(false);

            assertFalse(mapper.toDomain(entity).ativa());
        }

        @Test
        void recusaFusoQuePersistiuInvalido() {
            SalaEntity entity = entidade();
            entity.setFusoHorario("America/Pirapora");

            assertThrows(FusoHorarioInvalidoException.class, () -> mapper.toDomain(entity));
        }

        @Test
        void recusaNomeQuePersistiuVazio() {
            SalaEntity entity = entidade();
            entity.setNome("   ");

            assertThrows(SalaInvalidaException.class, () -> mapper.toDomain(entity));
        }
    }

    @Nested
    @DisplayName("domínio → entidade")
    class ParaEntidade {

        @Test
        void copiaTodosOsCampos() {
            Sala sala = new Sala(new SalaId(SALA_UUID), "Sala Londres", new Capacidade(4),
                    FusoHorario.of("Europe/London"), true);

            SalaEntity entity = mapper.toEntity(sala);

            assertAll(
                    () -> assertEquals(SALA_UUID, entity.getId()),
                    () -> assertEquals("Sala Londres", entity.getNome()),
                    () -> assertEquals(4, entity.getCapacidade()),
                    () -> assertEquals("Europe/London", entity.getFusoHorario()),
                    () -> assertTrue(entity.isAtiva()));
        }
    }

    @Nested
    @DisplayName("ida e volta")
    class IdaEVolta {

        @Test
        void preservaASala() {
            Sala original = mapper.toDomain(entidade());

            Sala reconstruida = mapper.toDomain(mapper.toEntity(original));

            assertAll(
                    () -> assertEquals(original.id(), reconstruida.id()),
                    () -> assertEquals(original.nome(), reconstruida.nome()),
                    () -> assertEquals(original.capacidade(), reconstruida.capacidade()),
                    () -> assertEquals(original.fuso(), reconstruida.fuso()),
                    () -> assertEquals(original.ativa(), reconstruida.ativa()));
        }
    }
}
