package brenner.edu.reservas.core.usecase;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.exceptions.CheckInAntecipadoException;
import brenner.edu.reservas.core.domain.exceptions.ReservaExpiradaException;
import brenner.edu.reservas.core.domain.exceptions.TransicaoDeStatusInvalidaException;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.port.SaveReservaPort;
import brenner.edu.reservas.core.usecase.commands.RealizarCheckInCommand;
import brenner.edu.reservas.core.usecase.exceptions.GetDataHoraAtualUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetReservaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.ReservaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.SalaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.SaveReservaUnavailableException;
import brenner.edu.reservas.shared.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealizarCheckInUseCaseTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID RESERVA_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final ReservaId RESERVA_ID = new ReservaId(RESERVA_UUID);
    private static final SalaId SALA_ID = SalaId.novo();
    private static final Sala SALA =
            new Sala(SALA_ID, "Sala Pirapora", new Capacidade(10), SAO_PAULO, true);

    private static final Periodo PERIODO = new Periodo(em("2026-09-15T14:00"), em("2026-09-15T15:30"));
    private static final DataHora NA_JANELA = em("2026-09-15T14:00");
    private static final DataHora DEPOIS_DA_JANELA = em("2026-09-15T14:16");

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static Reserva reservaCom(StatusReserva status) {
        return Reserva.reconstituir(RESERVA_ID, SALA_ID, new Email("ana@empresa.com"), PERIODO,
                new QuantidadeParticipantes(4), status, em("2026-09-14T10:00"));
    }

    private static RealizarCheckInCommand comando() {
        return new RealizarCheckInCommand(RESERVA_UUID);
    }

    private GetReservaPort getReservaPort = id -> Result.of(reservaCom(StatusReserva.PENDENTE));
    private GetSalaPort getSalaPort = id -> Result.of(SALA);
    private GetDataHoraAtualPort getDataHoraAtualPort = fuso -> Result.of(NA_JANELA);
    private SaveReservaPort saveReservaPort = Result::of;

    private RealizarCheckInUseCase useCase() {
        return new RealizarCheckInUseCase(getReservaPort, getSalaPort, getDataHoraAtualPort, saveReservaPort);
    }

    @Nested
    @DisplayName("caminho feliz")
    class CaminhoFeliz {

        @Test
        void confirmaAReserva() {
            Reserva reserva = useCase().execute(comando());

            assertAll(
                    () -> assertEquals(RESERVA_ID, reserva.id()),
                    () -> assertEquals(StatusReserva.CONFIRMADA, reserva.status()));
        }

        @Test
        void gravaAReservaConfirmada() {
            AtomicReference<Reserva> gravada = new AtomicReference<>();
            saveReservaPort = reserva -> {
                gravada.set(reserva);
                return Result.of(reserva);
            };

            useCase().execute(comando());

            assertEquals(StatusReserva.CONFIRMADA, gravada.get().status());
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
        void buscaAReservaPedidaNoComando() {
            AtomicReference<ReservaId> consultada = new AtomicReference<>();
            getReservaPort = id -> {
                consultada.set(id);
                return Result.of(reservaCom(StatusReserva.PENDENTE));
            };

            useCase().execute(comando());

            assertEquals(RESERVA_ID, consultada.get());
        }

        @Test
        void pedeOAgoraNoFusoDaSalaDaReserva() {
            AtomicReference<SalaId> salaConsultada = new AtomicReference<>();
            AtomicReference<FusoHorario> fusoPedido = new AtomicReference<>();
            getSalaPort = id -> {
                salaConsultada.set(id);
                return Result.of(SALA);
            };
            getDataHoraAtualPort = fuso -> {
                fusoPedido.set(fuso);
                return Result.of(NA_JANELA);
            };

            useCase().execute(comando());

            assertAll(
                    () -> assertEquals(SALA_ID, salaConsultada.get()),
                    () -> assertEquals(SAO_PAULO, fusoPedido.get()));
        }
    }

    @Nested
    @DisplayName("check-in fora da janela: expiração grava e falha")
    class Expiracao {

        @Test
        void propagaReservaExpirada() {
            getDataHoraAtualPort = fuso -> Result.of(DEPOIS_DA_JANELA);

            assertThrows(ReservaExpiradaException.class, () -> useCase().execute(comando()));
        }

        @Test
        void gravaAReservaExpiradaAntesDePropagar() {
            getDataHoraAtualPort = fuso -> Result.of(DEPOIS_DA_JANELA);
            AtomicReference<Reserva> gravada = new AtomicReference<>();
            saveReservaPort = reserva -> {
                gravada.set(reserva);
                return Result.of(reserva);
            };

            assertThrows(ReservaExpiradaException.class, () -> useCase().execute(comando()));

            assertAll(
                    () -> assertEquals(RESERVA_ID, gravada.get().id()),
                    () -> assertEquals(StatusReserva.EXPIRADA, gravada.get().status()));
        }

        @Test
        void indisponibilidadeNaGravacaoDaExpiracaoPrevaleceSobreAExpiracao() {
            getDataHoraAtualPort = fuso -> Result.of(DEPOIS_DA_JANELA);
            saveReservaPort = reserva -> Result.failure(new IllegalStateException("banco fora do ar"));

            assertThrows(SaveReservaUnavailableException.class, () -> useCase().execute(comando()));
        }

        @Test
        void aTransacaoNaoRevertePelaReservaExpirada() throws NoSuchMethodException {
            Transactional transacional = RealizarCheckInUseCase.class
                    .getMethod("execute", RealizarCheckInCommand.class)
                    .getAnnotation(Transactional.class);

            assertTrue(Arrays.asList(transacional.noRollbackFor()).contains(ReservaExpiradaException.class),
                    "sem noRollbackFor a gravação da reserva EXPIRADA seria desfeita pela exceção");
        }
    }

    @Nested
    @DisplayName("regras de domínio (422)")
    class RegrasDeDominio {

        @Test
        void propagaCheckInAntecipado() {
            getDataHoraAtualPort = fuso -> Result.of(em("2026-09-15T13:49"));

            assertThrows(CheckInAntecipadoException.class, () -> useCase().execute(comando()));
        }

        @Test
        void propagaStatusQueNaoAdmiteCheckIn() {
            getReservaPort = id -> Result.of(reservaCom(StatusReserva.CONFIRMADA));

            assertThrows(TransicaoDeStatusInvalidaException.class, () -> useCase().execute(comando()));
        }

        @Test
        void naoGravaQuandoOCheckInEAntecipado() {
            List<Reserva> gravadas = new ArrayList<>();
            saveReservaPort = reserva -> {
                gravadas.add(reserva);
                return Result.of(reserva);
            };
            getDataHoraAtualPort = fuso -> Result.of(em("2026-09-15T09:00"));

            assertThrows(CheckInAntecipadoException.class, () -> useCase().execute(comando()));
            assertTrue(gravadas.isEmpty());
        }

        @Test
        void naoGravaQuandoOStatusNaoAdmiteCheckIn() {
            List<Reserva> gravadas = new ArrayList<>();
            saveReservaPort = reserva -> {
                gravadas.add(reserva);
                return Result.of(reserva);
            };
            getReservaPort = id -> Result.of(reservaCom(StatusReserva.CANCELADA));

            assertThrows(TransicaoDeStatusInvalidaException.class, () -> useCase().execute(comando()));
            assertTrue(gravadas.isEmpty());
        }
    }

    @Nested
    @DisplayName("recursos ausentes (404)")
    class Ausentes {

        @Test
        void lancaReservaNotFoundQuandoAReservaNaoExiste() {
            getReservaPort = id -> Result.empty();

            ReservaNotFoundException excecao =
                    assertThrows(ReservaNotFoundException.class, () -> useCase().execute(comando()));

            assertTrue(excecao.getMessage().contains(RESERVA_UUID.toString()));
        }

        @Test
        void lancaSalaNotFoundQuandoASalaDaReservaNaoExiste() {
            getSalaPort = id -> Result.empty();

            assertThrows(SalaNotFoundException.class, () -> useCase().execute(comando()));
        }
    }

    @Nested
    @DisplayName("ports indisponíveis (503)")
    class PortsIndisponiveis {

        private static final RuntimeException CAUSA = new IllegalStateException("banco fora do ar");

        @Test
        void reservaIndisponivel() {
            getReservaPort = id -> Result.failure(CAUSA);

            GetReservaUnavailableException excecao =
                    assertThrows(GetReservaUnavailableException.class, () -> useCase().execute(comando()));

            assertSame(CAUSA, excecao.getCause());
        }

        @Test
        void salaIndisponivel() {
            getSalaPort = id -> Result.failure(CAUSA);

            assertThrows(GetSalaUnavailableException.class, () -> useCase().execute(comando()));
        }

        @Test
        void relogioIndisponivel() {
            getDataHoraAtualPort = fuso -> Result.failure(CAUSA);

            assertThrows(GetDataHoraAtualUnavailableException.class, () -> useCase().execute(comando()));
        }

        @Test
        void relogioVazioTambemEIndisponibilidade() {
            getDataHoraAtualPort = fuso -> Result.empty();

            assertThrows(GetDataHoraAtualUnavailableException.class, () -> useCase().execute(comando()));
        }

        @Test
        void gravacaoIndisponivel() {
            saveReservaPort = reserva -> Result.failure(CAUSA);

            SaveReservaUnavailableException excecao =
                    assertThrows(SaveReservaUnavailableException.class, () -> useCase().execute(comando()));

            assertSame(CAUSA, excecao.getCause());
        }

        @Test
        void gravacaoVaziaTambemEIndisponibilidade() {
            saveReservaPort = reserva -> Result.empty();

            assertThrows(SaveReservaUnavailableException.class, () -> useCase().execute(comando()));
        }
    }
}
