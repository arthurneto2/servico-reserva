package brenner.edu.reservas.output.persistence.adapters;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.output.persistence.ReservaRepository;
import brenner.edu.reservas.output.persistence.SalaRepository;
import brenner.edu.reservas.output.persistence.entities.ReservaEntity;
import brenner.edu.reservas.output.persistence.entities.SalaEntity;
import brenner.edu.reservas.output.persistence.mappers.ReservaMapper;
import brenner.edu.reservas.output.persistence.mappers.SalaMapper;
import brenner.edu.reservas.shared.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato comum dos adapters de persistência: os três estados do {@code Result}, incluindo o que
 * acontece quando o dado gravado no banco não satisfaz mais as invariantes do domínio.
 */
class PersistenceAdaptersTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID SALA_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVA_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final SalaId SALA_ID = new SalaId(SALA_UUID);
    private static final ReservaId RESERVA_ID = new ReservaId(RESERVA_UUID);
    private static final RuntimeException BANCO_FORA = new IllegalStateException("banco fora do ar");

    private final SalaRepository salaRepository = mock(SalaRepository.class);
    private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    private final SalaMapper salaMapper = new SalaMapper();
    private final ReservaMapper reservaMapper = new ReservaMapper();

    private static Instant instante(String local) {
        return LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant();
    }

    private static SalaEntity salaEntity() {
        SalaEntity entity = new SalaEntity();
        entity.setId(SALA_UUID);
        entity.setNome("Sala Pirapora");
        entity.setCapacidade(8);
        entity.setFusoHorario("America/Sao_Paulo");
        entity.setAtiva(true);
        return entity;
    }

    private static ReservaEntity reservaEntity() {
        ReservaEntity entity = new ReservaEntity();
        entity.setId(RESERVA_UUID);
        entity.setSalaId(SALA_UUID);
        entity.setOrganizadorEmail("ana@empresa.com");
        entity.setInicio(instante("2026-09-15T14:00"));
        entity.setFim(instante("2026-09-15T15:30"));
        entity.setQuantidadeParticipantes(6);
        entity.setStatus("PENDENTE");
        entity.setCriadaEm(instante("2026-09-14T10:00"));
        return entity;
    }

    private static Reserva reservaDominio() {
        return Reserva.reconstituir(RESERVA_ID, SALA_ID, new Email("ana@empresa.com"),
                new Periodo(new DataHora(instante("2026-09-15T14:00"), SAO_PAULO),
                        new DataHora(instante("2026-09-15T15:30"), SAO_PAULO)),
                new QuantidadeParticipantes(6), StatusReserva.PENDENTE,
                new DataHora(instante("2026-09-14T10:00"), SAO_PAULO));
    }

    @Nested
    @DisplayName("GetSalaAdapter")
    class GetSala {

        private final GetSalaAdapter adapter = new GetSalaAdapter(salaRepository, salaMapper);

        @Test
        void devolveASalaQuandoElaExiste() {
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(salaEntity()));

            Sala sala = adapter.getById(SALA_ID).getAvailableValueOrElseThrow(RuntimeException::new).orElseThrow();

            assertAll(
                    () -> assertEquals(SALA_ID, sala.id()),
                    () -> assertEquals("Sala Pirapora", sala.nome()),
                    () -> assertEquals(SAO_PAULO, sala.fuso()));
        }

        @Test
        void devolveVazioQuandoElaNaoExiste() {
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.empty());

            Result<Sala> resultado = adapter.getById(SALA_ID);

            assertAll(
                    () -> assertTrue(resultado.isEmpty()),
                    () -> assertFalse(resultado.isFailure()));
        }

        @Test
        void devolveFalhaQuandoORepositorioExplode() {
            when(salaRepository.findById(SALA_UUID)).thenThrow(BANCO_FORA);

            assertTrue(adapter.getById(SALA_ID).isFailure());
        }

        @Test
        void devolveFalhaQuandoODadoGravadoViolaUmaInvariante() {
            SalaEntity entity = salaEntity();
            entity.setFusoHorario("America/Pirapora");
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(entity));

            assertTrue(adapter.getById(SALA_ID).isFailure());
        }

        @Test
        void idNuloEFalhaTecnicaENaoAusencia() {
            assertTrue(adapter.getById(null).isFailure());
        }
    }

    @Nested
    @DisplayName("GetReservaAdapter")
    class GetReserva {

        private final GetReservaAdapter adapter =
                new GetReservaAdapter(reservaRepository, salaRepository, reservaMapper, salaMapper);

        @Test
        void devolveAReservaNoFusoDaSalaDela() {
            when(reservaRepository.findById(RESERVA_UUID)).thenReturn(Optional.of(reservaEntity()));
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(salaEntity()));

            Reserva reserva = adapter.getById(RESERVA_ID)
                    .getAvailableValueOrElseThrow(RuntimeException::new).orElseThrow();

            assertAll(
                    () -> assertEquals(RESERVA_ID, reserva.id()),
                    () -> assertEquals(SAO_PAULO, reserva.periodo().inicio().fusoHorario()),
                    () -> assertEquals(StatusReserva.PENDENTE, reserva.status()));
        }

        @Test
        void devolveVazioQuandoAReservaNaoExiste() {
            when(reservaRepository.findById(RESERVA_UUID)).thenReturn(Optional.empty());

            assertTrue(adapter.getById(RESERVA_ID).isEmpty());
        }

        @Test
        void salaAusenteEFalhaTecnicaENaoReservaAusente() {
            // A FK garante a sala; se ela sumiu, o banco está inconsistente — isso é 503, não 404.
            when(reservaRepository.findById(RESERVA_UUID)).thenReturn(Optional.of(reservaEntity()));
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.empty());

            assertTrue(adapter.getById(RESERVA_ID).isFailure());
        }

        @Test
        void devolveFalhaQuandoORepositorioExplode() {
            when(reservaRepository.findById(RESERVA_UUID)).thenThrow(BANCO_FORA);

            assertTrue(adapter.getById(RESERVA_ID).isFailure());
        }

        @Test
        void devolveFalhaQuandoODadoGravadoViolaUmaInvariante() {
            ReservaEntity entity = reservaEntity();
            entity.setOrganizadorEmail("ana(at)empresa");
            when(reservaRepository.findById(RESERVA_UUID)).thenReturn(Optional.of(entity));
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(salaEntity()));

            assertTrue(adapter.getById(RESERVA_ID).isFailure());
        }
    }

    @Nested
    @DisplayName("GetReservasAtivasDaSalaAdapter")
    class GetReservasAtivasDaSala {

        private final GetReservasAtivasDaSalaAdapter adapter =
                new GetReservasAtivasDaSalaAdapter(reservaRepository, salaRepository, reservaMapper, salaMapper);

        private final DataHora inicio = new DataHora(instante("2026-09-15T00:00"), SAO_PAULO);
        private final DataHora fim = new DataHora(instante("2026-09-16T00:00"), SAO_PAULO);

        @Test
        void devolveAsReservasDaJanela() {
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(salaEntity()));
            when(reservaRepository.buscarNaJanela(eq(SALA_UUID), any(), any(), any()))
                    .thenReturn(List.of(reservaEntity()));

            List<Reserva> reservas = adapter.get(SALA_ID, inicio, fim)
                    .getAvailableValueOrElseThrow(RuntimeException::new).orElseThrow();

            assertAll(
                    () -> assertEquals(1, reservas.size()),
                    () -> assertEquals(RESERVA_ID, reservas.get(0).id()),
                    () -> assertEquals(SAO_PAULO, reservas.get(0).periodo().inicio().fusoHorario()));
        }

        @Test
        void filtraPendentesEConfirmadasNoProprioAdapter() {
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(salaEntity()));
            when(reservaRepository.buscarNaJanela(any(), any(), any(), any())).thenReturn(List.of());

            adapter.get(SALA_ID, inicio, fim);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<String>> status = ArgumentCaptor.forClass(Collection.class);
            verify(reservaRepository).buscarNaJanela(eq(SALA_UUID), status.capture(),
                    eq(inicio.instante()), eq(fim.instante()));
            assertEquals(List.of("PENDENTE", "CONFIRMADA"), List.copyOf(status.getValue()));
        }

        @Test
        void agendaLivreEListaVaziaENaoResultadoVazio() {
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(salaEntity()));
            when(reservaRepository.buscarNaJanela(any(), any(), any(), any())).thenReturn(List.of());

            Result<List<Reserva>> resultado = adapter.get(SALA_ID, inicio, fim);

            assertAll(
                    () -> assertFalse(resultado.isEmpty()),
                    () -> assertTrue(resultado.getAvailableValueOrElseThrow(RuntimeException::new)
                            .orElseThrow().isEmpty()));
        }

        @Test
        void devolveFalhaQuandoORepositorioExplode() {
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.of(salaEntity()));
            when(reservaRepository.buscarNaJanela(any(), any(), any(), any())).thenThrow(BANCO_FORA);

            assertTrue(adapter.get(SALA_ID, inicio, fim).isFailure());
        }

        @Test
        void salaAusenteEFalhaTecnica() {
            when(salaRepository.findById(SALA_UUID)).thenReturn(Optional.empty());

            assertTrue(adapter.get(SALA_ID, inicio, fim).isFailure());
        }
    }

    @Nested
    @DisplayName("SaveReservaAdapter")
    class SaveReserva {

        private final SaveReservaAdapter adapter = new SaveReservaAdapter(reservaRepository, reservaMapper);

        @Test
        void gravaTodosOsCamposDaReserva() {
            when(reservaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            adapter.save(reservaDominio());

            ArgumentCaptor<ReservaEntity> gravada = ArgumentCaptor.forClass(ReservaEntity.class);
            verify(reservaRepository).save(gravada.capture());
            assertAll(
                    () -> assertEquals(RESERVA_UUID, gravada.getValue().getId()),
                    () -> assertEquals(SALA_UUID, gravada.getValue().getSalaId()),
                    () -> assertEquals("ana@empresa.com", gravada.getValue().getOrganizadorEmail()),
                    () -> assertEquals("PENDENTE", gravada.getValue().getStatus()),
                    () -> assertEquals(instante("2026-09-15T14:00"), gravada.getValue().getInicio()));
        }

        @Test
        void devolveOEstadoQueOBancoConfirmou() {
            ReservaEntity confirmada = reservaEntity();
            confirmada.setStatus("CONFIRMADA");
            when(reservaRepository.save(any())).thenReturn(confirmada);

            Reserva reserva = adapter.save(reservaDominio())
                    .getAvailableValueOrElseThrow(RuntimeException::new).orElseThrow();

            assertEquals(StatusReserva.CONFIRMADA, reserva.status());
        }

        @Test
        void remontaARespostaNoFusoDaReservaGravada() {
            // O fuso não precisa de consulta: ele veio junto da reserva que está sendo gravada.
            when(reservaRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

            Reserva reserva = adapter.save(reservaDominio())
                    .getAvailableValueOrElseThrow(RuntimeException::new).orElseThrow();

            assertEquals(SAO_PAULO, reserva.periodo().inicio().fusoHorario());
        }

        @Test
        void devolveFalhaQuandoORepositorioExplode() {
            when(reservaRepository.save(any())).thenThrow(BANCO_FORA);

            Result<Reserva> resultado = adapter.save(reservaDominio());

            assertTrue(resultado.isFailure());
        }

        @Test
        void devolveFalhaQuandoAReservaENula() {
            assertTrue(adapter.save(null).isFailure());
        }

        @Test
        void aCausaTecnicaChegaInteiraAoUseCase() {
            when(reservaRepository.save(any())).thenThrow(BANCO_FORA);

            RuntimeException propagada = org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> adapter.save(reservaDominio()).getAvailableValueOrElseThrow(RuntimeException::new));

            assertSame(BANCO_FORA, propagada.getCause());
        }
    }
}
