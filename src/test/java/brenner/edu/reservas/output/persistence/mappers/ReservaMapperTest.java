package brenner.edu.reservas.output.persistence.mappers;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.exceptions.EmailInvalidoException;
import brenner.edu.reservas.core.domain.exceptions.PeriodoInvalidoException;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.output.persistence.entities.ReservaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaMapperTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final FusoHorario NOVA_YORK = FusoHorario.of("America/New_York");
    private static final UUID RESERVA_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SALA_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ReservaMapper mapper = new ReservaMapper();

    private static Instant instante(String local, FusoHorario fuso) {
        return LocalDateTime.parse(local).atZone(fuso.value()).toInstant();
    }

    private static ReservaEntity entidade() {
        ReservaEntity entity = new ReservaEntity();
        entity.setId(RESERVA_UUID);
        entity.setSalaId(SALA_UUID);
        entity.setOrganizadorEmail("ana@empresa.com");
        entity.setInicio(instante("2026-09-15T14:00", SAO_PAULO));
        entity.setFim(instante("2026-09-15T15:30", SAO_PAULO));
        entity.setQuantidadeParticipantes(6);
        entity.setStatus("PENDENTE");
        entity.setCriadaEm(instante("2026-09-14T10:00", SAO_PAULO));
        return entity;
    }

    private static Reserva dominio() {
        return Reserva.reconstituir(new ReservaId(RESERVA_UUID), new SalaId(SALA_UUID),
                new Email("ana@empresa.com"),
                new Periodo(new DataHora(instante("2026-09-15T14:00", SAO_PAULO), SAO_PAULO),
                        new DataHora(instante("2026-09-15T15:30", SAO_PAULO), SAO_PAULO)),
                new QuantidadeParticipantes(6), StatusReserva.PENDENTE,
                new DataHora(instante("2026-09-14T10:00", SAO_PAULO), SAO_PAULO));
    }

    @Nested
    @DisplayName("entidade → domínio")
    class ParaDominio {

        @Test
        void reconstroiTodosOsCampos() {
            Reserva reserva = mapper.toDomain(entidade(), SAO_PAULO);

            assertAll(
                    () -> assertEquals(new ReservaId(RESERVA_UUID), reserva.id()),
                    () -> assertEquals(new SalaId(SALA_UUID), reserva.salaId()),
                    () -> assertEquals(new Email("ana@empresa.com"), reserva.organizador()),
                    () -> assertEquals(new QuantidadeParticipantes(6), reserva.participantes()),
                    () -> assertEquals(StatusReserva.PENDENTE, reserva.status()),
                    () -> assertEquals(instante("2026-09-15T14:00", SAO_PAULO),
                            reserva.periodo().inicio().instante()),
                    () -> assertEquals(instante("2026-09-15T15:30", SAO_PAULO),
                            reserva.periodo().fim().instante()),
                    () -> assertEquals(instante("2026-09-14T10:00", SAO_PAULO),
                            reserva.criadaEm().instante()));
        }

        @Test
        void expressaOsInstantesNoFusoRecebido() {
            Reserva reserva = mapper.toDomain(entidade(), SAO_PAULO);

            assertAll(
                    () -> assertEquals(SAO_PAULO, reserva.periodo().inicio().fusoHorario()),
                    () -> assertEquals(SAO_PAULO, reserva.criadaEm().fusoHorario()),
                    () -> assertEquals(LocalDateTime.parse("2026-09-15T14:00").toLocalTime(),
                            reserva.periodo().inicio().hora()));
        }

        @Test
        void oMesmoInstanteEmOutroFusoViraOutraHoraLocal() {
            ReservaEntity entity = entidade();
            entity.setInicio(instante("2026-09-15T14:00", NOVA_YORK));
            entity.setFim(instante("2026-09-15T15:30", NOVA_YORK));

            Reserva reserva = mapper.toDomain(entity, NOVA_YORK);

            assertEquals(LocalDateTime.parse("2026-09-15T14:00").toLocalTime(),
                    reserva.periodo().inicio().hora());
        }

        @Test
        void recusaEmailQuePersistiuInvalido() {
            ReservaEntity entity = entidade();
            entity.setOrganizadorEmail("ana(at)empresa");

            assertThrows(EmailInvalidoException.class, () -> mapper.toDomain(entity, SAO_PAULO));
        }

        @Test
        void recusaPeriodoQuePersistiuForaDoHorarioDeFuncionamento() {
            ReservaEntity entity = entidade();
            entity.setInicio(instante("2026-09-15T06:00", SAO_PAULO));
            entity.setFim(instante("2026-09-15T07:00", SAO_PAULO));

            assertThrows(PeriodoInvalidoException.class, () -> mapper.toDomain(entity, SAO_PAULO));
        }

        @Test
        void recusaStatusDesconhecido() {
            ReservaEntity entity = entidade();
            entity.setStatus("ARQUIVADA");

            assertThrows(IllegalArgumentException.class, () -> mapper.toDomain(entity, SAO_PAULO));
        }
    }

    @Nested
    @DisplayName("domínio → entidade")
    class ParaEntidade {

        @Test
        void copiaTodosOsCampos() {
            ReservaEntity entity = mapper.toEntity(dominio());

            assertAll(
                    () -> assertEquals(RESERVA_UUID, entity.getId()),
                    () -> assertEquals(SALA_UUID, entity.getSalaId()),
                    () -> assertEquals("ana@empresa.com", entity.getOrganizadorEmail()),
                    () -> assertEquals(instante("2026-09-15T14:00", SAO_PAULO), entity.getInicio()),
                    () -> assertEquals(instante("2026-09-15T15:30", SAO_PAULO), entity.getFim()),
                    () -> assertEquals(6, entity.getQuantidadeParticipantes()),
                    () -> assertEquals("PENDENTE", entity.getStatus()),
                    () -> assertEquals(instante("2026-09-14T10:00", SAO_PAULO), entity.getCriadaEm()));
        }

        @Test
        void gravaInstantesENaoHorasLocais() {
            // A hora local só existe na borda; no banco fica o instante, e é isso que permite
            // comparar reservas de salas em fusos diferentes.
            ReservaEntity entity = mapper.toEntity(dominio());

            assertEquals(Instant.parse("2026-09-15T17:00:00Z"), entity.getInicio());
        }
    }

    @Nested
    @DisplayName("ida e volta")
    class IdaEVolta {

        @Test
        void preservaAReserva() {
            Reserva original = dominio();

            Reserva reconstruida = mapper.toDomain(mapper.toEntity(original), SAO_PAULO);

            assertAll(
                    () -> assertEquals(original.id(), reconstruida.id()),
                    () -> assertEquals(original.salaId(), reconstruida.salaId()),
                    () -> assertEquals(original.organizador(), reconstruida.organizador()),
                    () -> assertEquals(original.periodo(), reconstruida.periodo()),
                    () -> assertEquals(original.participantes(), reconstruida.participantes()),
                    () -> assertEquals(original.status(), reconstruida.status()),
                    () -> assertEquals(original.criadaEm(), reconstruida.criadaEm()));
        }
    }
}
