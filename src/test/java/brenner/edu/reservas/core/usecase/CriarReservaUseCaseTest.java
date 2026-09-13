package brenner.edu.reservas.core.usecase;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.exceptions.CapacidadeExcedidaException;
import brenner.edu.reservas.core.domain.exceptions.ConflitoDeHorarioException;
import brenner.edu.reservas.core.domain.exceptions.EmailInvalidoException;
import brenner.edu.reservas.core.domain.exceptions.PeriodoInvalidoException;
import brenner.edu.reservas.core.domain.exceptions.SalaInativaException;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservasAtivasDaSalaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.port.NotificarOrganizadorPort;
import brenner.edu.reservas.core.port.SaveReservaPort;
import brenner.edu.reservas.core.usecase.commands.CriarReservaCommand;
import brenner.edu.reservas.core.usecase.exceptions.GetDataHoraAtualUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetReservasAtivasDaSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.NotificarOrganizadorUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.SalaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.SaveReservaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.UnavailableException;
import brenner.edu.reservas.shared.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriarReservaUseCaseTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID SALA_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final SalaId SALA_ID = new SalaId(SALA_UUID);
    private static final Sala SALA =
            new Sala(SALA_ID, "Sala Pirapora", new Capacidade(10), SAO_PAULO, true);

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 9, 15, 14, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2026, 9, 15, 15, 30);
    private static final String ORGANIZADOR = "Ana@Empresa.com";
    private static final DataHora AGORA = em(LocalDateTime.of(2026, 9, 15, 9, 0));

    private static DataHora em(LocalDateTime local) {
        return new DataHora(local.atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static CriarReservaCommand comando() {
        return new CriarReservaCommand(SALA_UUID, ORGANIZADOR, INICIO, FIM, 6);
    }

    private GetSalaPort getSalaPort = id -> Result.of(SALA);
    private GetDataHoraAtualPort getDataHoraAtualPort = fuso -> Result.of(AGORA);
    private GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.of(List.of());
    private SaveReservaPort saveReservaPort = Result::of;
    private NotificarOrganizadorPort notificarOrganizadorPort = reserva -> Result.empty();

    private CriarReservaUseCase useCase() {
        return new CriarReservaUseCase(getSalaPort,
                getDataHoraAtualPort,
                getReservasAtivasDaSalaPort,
                saveReservaPort,
                notificarOrganizadorPort);
    }

    private static Reserva reservaOcupando(String inicio, String fim) {
        Periodo periodo = new Periodo(em(LocalDateTime.parse("2026-09-15T" + inicio)),
                em(LocalDateTime.parse("2026-09-15T" + fim)));
        return Reserva.solicitar(ReservaId.novo(), SALA_ID, new Email("outro@empresa.com"),
                periodo, new QuantidadeParticipantes(2), AGORA);
    }

    @Nested
    @DisplayName("caminho feliz")
    class CaminhoFeliz {

        @Test
        void criaReservaPendenteNoFusoDaSala() {
            Reserva reserva = useCase().execute(comando());

            assertAll(
                    () -> assertEquals(SALA_ID, reserva.salaId()),
                    () -> assertEquals(new Email("ana@empresa.com"), reserva.organizador()),
                    () -> assertEquals(StatusReserva.PENDENTE, reserva.status()),
                    () -> assertEquals(new QuantidadeParticipantes(6), reserva.participantes()),
                    () -> assertEquals(AGORA, reserva.criadaEm()),
                    () -> assertEquals(em(INICIO), reserva.periodo().inicio()),
                    () -> assertEquals(em(FIM), reserva.periodo().fim()),
                    () -> assertEquals(SAO_PAULO, reserva.periodo().inicio().fusoHorario()));
        }

        @Test
        void devolveAReservaDevolvidaPeloPortDeGravacao() {
            AtomicReference<Reserva> gravada = new AtomicReference<>();
            saveReservaPort = reserva -> {
                gravada.set(reserva);
                return Result.of(reserva);
            };

            Reserva devolvida = useCase().execute(comando());

            assertSame(gravada.get(), devolvida);
        }

        @Test
        void pedeOAgoraNoFusoDaSala() {
            AtomicReference<FusoHorario> pedido = new AtomicReference<>();
            getDataHoraAtualPort = fuso -> {
                pedido.set(fuso);
                return Result.of(AGORA);
            };

            useCase().execute(comando());

            assertEquals(SAO_PAULO, pedido.get());
        }

        @Test
        void consultaAgendaDoDiaInteiroDaSalaNoFusoDela() {
            AtomicReference<SalaId> salaConsultada = new AtomicReference<>();
            AtomicReference<DataHora> inicioConsultado = new AtomicReference<>();
            AtomicReference<DataHora> fimConsultado = new AtomicReference<>();
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> {
                salaConsultada.set(id);
                inicioConsultado.set(inicio);
                fimConsultado.set(fim);
                return Result.of(List.of());
            };

            useCase().execute(comando());

            assertAll(
                    () -> assertEquals(SALA_ID, salaConsultada.get()),
                    () -> assertEquals(em(LocalDateTime.of(2026, 9, 15, 0, 0)), inicioConsultado.get()),
                    () -> assertEquals(em(LocalDateTime.of(2026, 9, 16, 0, 0)), fimConsultado.get()));
        }

        @Test
        void notificaOOrganizadorComAReservaGravada() {
            AtomicReference<Reserva> notificada = new AtomicReference<>();
            notificarOrganizadorPort = reserva -> {
                notificada.set(reserva);
                return Result.empty();
            };

            Reserva devolvida = useCase().execute(comando());

            assertSame(devolvida, notificada.get());
        }

        @Test
        void tratraAgendaVaziaComoAgendaLivre() {
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.empty();

            assertEquals(StatusReserva.PENDENTE, useCase().execute(comando()).status());
        }
    }

    @Nested
    @DisplayName("regras de domínio (422)")
    class RegrasDeDominio {

        @Test
        void propagaConflitoDeHorario() {
            getReservasAtivasDaSalaPort =
                    (id, inicio, fim) -> Result.of(List.of(reservaOcupando("14:30", "16:00")));

            assertThrows(ConflitoDeHorarioException.class, () -> useCase().execute(comando()));
        }

        @Test
        void propagaSalaInativa() {
            getSalaPort = id -> Result.of(
                    new Sala(SALA_ID, "Sala Pirapora", new Capacidade(10), SAO_PAULO, false));

            assertThrows(SalaInativaException.class, () -> useCase().execute(comando()));
        }

        @Test
        void propagaCapacidadeExcedida() {
            CriarReservaCommand command =
                    new CriarReservaCommand(SALA_UUID, ORGANIZADOR, INICIO, FIM, 11);

            assertThrows(CapacidadeExcedidaException.class, () -> useCase().execute(command));
        }

        @Test
        void propagaEmailInvalido() {
            CriarReservaCommand command =
                    new CriarReservaCommand(SALA_UUID, "ana(at)empresa", INICIO, FIM, 6);

            assertThrows(EmailInvalidoException.class, () -> useCase().execute(command));
        }

        @Test
        void propagaPeriodoInvalido() {
            CriarReservaCommand command =
                    new CriarReservaCommand(SALA_UUID, ORGANIZADOR, FIM, INICIO, 6);

            assertThrows(PeriodoInvalidoException.class, () -> useCase().execute(command));
        }

        @Test
        void naoGravaQuandoODominioRecusa() {
            List<Reserva> gravadas = new ArrayList<>();
            saveReservaPort = reserva -> {
                gravadas.add(reserva);
                return Result.of(reserva);
            };
            getReservasAtivasDaSalaPort =
                    (id, inicio, fim) -> Result.of(List.of(reservaOcupando("14:30", "16:00")));

            assertThrows(ConflitoDeHorarioException.class, () -> useCase().execute(comando()));
            assertTrue(gravadas.isEmpty());
        }
    }

    @Nested
    @DisplayName("sala ausente (404)")
    class SalaAusente {

        @Test
        void lancaSalaNotFoundQuandoASalaNaoExiste() {
            getSalaPort = id -> Result.empty();

            SalaNotFoundException excecao =
                    assertThrows(SalaNotFoundException.class, () -> useCase().execute(comando()));

            assertTrue(excecao.getMessage().contains(SALA_UUID.toString()));
        }

        @Test
        void naoConsultaORelogioQuandoASalaNaoExiste() {
            getSalaPort = id -> Result.empty();
            List<FusoHorario> consultas = new ArrayList<>();
            getDataHoraAtualPort = fuso -> {
                consultas.add(fuso);
                return Result.of(AGORA);
            };

            assertThrows(SalaNotFoundException.class, () -> useCase().execute(comando()));
            assertTrue(consultas.isEmpty());
        }
    }

    @Nested
    @DisplayName("ports indisponíveis (503)")
    class PortsIndisponiveis {

        private static final RuntimeException CAUSA = new IllegalStateException("banco fora do ar");

        @Test
        void salaIndisponivel() {
            getSalaPort = id -> Result.failure(CAUSA);

            GetSalaUnavailableException excecao =
                    assertThrows(GetSalaUnavailableException.class, () -> useCase().execute(comando()));

            assertSame(CAUSA, excecao.getCause());
        }

        @Test
        void relogioIndisponivel() {
            getDataHoraAtualPort = fuso -> Result.failure(CAUSA);

            GetDataHoraAtualUnavailableException excecao = assertThrows(
                    GetDataHoraAtualUnavailableException.class, () -> useCase().execute(comando()));

            assertSame(CAUSA, excecao.getCause());
        }

        @Test
        void relogioVazioTambemEIndisponibilidade() {
            getDataHoraAtualPort = fuso -> Result.empty();

            assertThrows(GetDataHoraAtualUnavailableException.class, () -> useCase().execute(comando()));
        }

        @Test
        void agendaIndisponivel() {
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.failure(CAUSA);

            GetReservasAtivasDaSalaUnavailableException excecao = assertThrows(
                    GetReservasAtivasDaSalaUnavailableException.class, () -> useCase().execute(comando()));

            assertSame(CAUSA, excecao.getCause());
        }

        @Test
        void gravacaoIndisponivel() {
            saveReservaPort = reserva -> Result.failure(CAUSA);

            SaveReservaUnavailableException excecao = assertThrows(
                    SaveReservaUnavailableException.class, () -> useCase().execute(comando()));

            assertSame(CAUSA, excecao.getCause());
        }

        @Test
        void gravacaoVaziaTambemEIndisponibilidade() {
            saveReservaPort = reserva -> Result.empty();

            assertThrows(SaveReservaUnavailableException.class, () -> useCase().execute(comando()));
        }

        @Test
        void notificacaoIndisponivel() {
            notificarOrganizadorPort = reserva -> Result.failure(CAUSA);

            NotificarOrganizadorUnavailableException excecao = assertThrows(
                    NotificarOrganizadorUnavailableException.class, () -> useCase().execute(comando()));

            assertSame(CAUSA, excecao.getCause());
        }

        @Test
        void toda503EUmaUnavailableException() {
            getSalaPort = id -> Result.failure(CAUSA);

            assertTrue(assertThrows(RuntimeException.class, () -> useCase().execute(comando()))
                    instanceof UnavailableException);
        }

        @Test
        void notFoundNaoEUnavailable() {
            getSalaPort = id -> Result.empty();

            assertFalse(assertThrows(RuntimeException.class, () -> useCase().execute(comando()))
                    instanceof UnavailableException);
        }
    }
}
