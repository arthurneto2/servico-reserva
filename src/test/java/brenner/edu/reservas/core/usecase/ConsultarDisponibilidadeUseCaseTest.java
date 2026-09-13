package brenner.edu.reservas.core.usecase;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.exceptions.DataNoPassadoException;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.JanelaLivre;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservasAtivasDaSalaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.usecase.commands.ConsultarDisponibilidadeCommand;
import brenner.edu.reservas.core.usecase.exceptions.GetDataHoraAtualUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetReservasAtivasDaSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.SalaNotFoundException;
import brenner.edu.reservas.core.usecase.results.DisponibilidadeDaSala;
import brenner.edu.reservas.shared.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarDisponibilidadeUseCaseTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID SALA_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final SalaId SALA_ID = new SalaId(SALA_UUID);
    private static final Sala SALA =
            new Sala(SALA_ID, "Sala Pirapora", new Capacidade(10), SAO_PAULO, true);

    private static final LocalDate DIA = LocalDate.of(2026, 9, 15);
    private static final DataHora AGORA = em("2026-09-15T09:00");

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static JanelaLivre janela(String inicio, String fim) {
        return new JanelaLivre(em(inicio), em(fim));
    }

    private static Reserva reservaOcupando(String inicio, String fim) {
        return Reserva.reconstituir(ReservaId.novo(), SALA_ID, new Email("ana@empresa.com"),
                new Periodo(em(inicio), em(fim)), new QuantidadeParticipantes(4),
                StatusReserva.PENDENTE, em("2026-09-14T10:00"));
    }

    private static ConsultarDisponibilidadeCommand comando() {
        return new ConsultarDisponibilidadeCommand(SALA_UUID, DIA);
    }

    private GetSalaPort getSalaPort = id -> Result.of(SALA);
    private GetDataHoraAtualPort getDataHoraAtualPort = fuso -> Result.of(AGORA);
    private GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.of(List.of());

    private ConsultarDisponibilidadeUseCase useCase() {
        return new ConsultarDisponibilidadeUseCase(getSalaPort, getDataHoraAtualPort, getReservasAtivasDaSalaPort);
    }

    @Nested
    @DisplayName("caminho feliz")
    class CaminhoFeliz {

        @Test
        void descontaAsReservasDoHorarioDeFuncionamento() {
            getReservasAtivasDaSalaPort =
                    (id, inicio, fim) -> Result.of(List.of(reservaOcupando("2026-09-15T14:00", "2026-09-15T15:30")));

            DisponibilidadeDaSala disponibilidade = useCase().execute(comando());

            assertEquals(List.of(janela("2026-09-15T08:00", "2026-09-15T14:00"),
                            janela("2026-09-15T15:30", "2026-09-15T20:00")),
                    disponibilidade.horariosLivres());
        }

        @Test
        void diaSemReservasTemUmaUnicaJanela() {
            DisponibilidadeDaSala disponibilidade = useCase().execute(comando());

            assertEquals(List.of(janela("2026-09-15T08:00", "2026-09-15T20:00")),
                    disponibilidade.horariosLivres());
        }

        @Test
        void devolveASalaEADataConsultadas() {
            DisponibilidadeDaSala disponibilidade = useCase().execute(comando());

            assertAll(
                    () -> assertEquals(SALA, disponibilidade.sala()),
                    () -> assertEquals("Sala Pirapora", disponibilidade.sala().nome()),
                    () -> assertEquals(SAO_PAULO, disponibilidade.sala().fuso()),
                    () -> assertEquals(DIA, disponibilidade.data()));
        }

        @Test
        void devolveAsReservasQueOcupamODia() {
            Reserva reserva = reservaOcupando("2026-09-15T14:00", "2026-09-15T15:30");
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.of(List.of(reserva));

            DisponibilidadeDaSala disponibilidade = useCase().execute(comando());

            assertEquals(List.of(reserva), disponibilidade.reservas());
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
                    () -> assertEquals(em("2026-09-15T00:00"), inicioConsultado.get()),
                    () -> assertEquals(em("2026-09-16T00:00"), fimConsultado.get()));
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
        void tratraAgendaVaziaComoDiaTodoLivre() {
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.empty();

            DisponibilidadeDaSala disponibilidade = useCase().execute(comando());

            assertAll(
                    () -> assertEquals(List.of(janela("2026-09-15T08:00", "2026-09-15T20:00")),
                            disponibilidade.horariosLivres()),
                    () -> assertTrue(disponibilidade.reservas().isEmpty()));
        }

        @Test
        void oResultadoNaoSeDeixaAlterarPorQuemORecebe() {
            List<Reserva> doPort = new ArrayList<>(List.of(reservaOcupando("2026-09-15T14:00", "2026-09-15T15:30")));
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.of(doPort);

            DisponibilidadeDaSala disponibilidade = useCase().execute(comando());
            doPort.clear();

            assertEquals(1, disponibilidade.reservas().size());
        }
    }

    @Nested
    @DisplayName("data no passado (422)")
    class DataNoPassado {

        @Test
        void recusaDiaAnteriorAoDeHojeNaSala() {
            ConsultarDisponibilidadeCommand command =
                    new ConsultarDisponibilidadeCommand(SALA_UUID, DIA.minusDays(1));

            assertThrows(DataNoPassadoException.class, () -> useCase().execute(command));
        }

        @Test
        void aceitaODiaDeHojeNaSala() {
            assertDoesNotThrow(() -> useCase().execute(comando()));
        }

        @Test
        void aceitaDiaFuturo() {
            ConsultarDisponibilidadeCommand command =
                    new ConsultarDisponibilidadeCommand(SALA_UUID, DIA.plusDays(30));

            assertDoesNotThrow(() -> useCase().execute(command));
        }

        @Test
        void oHojeQueValeEODoFusoDaSala() {
            // 2026-09-15T23:00 em São Paulo já é 2026-09-16 em Lisboa: consultar o dia 16 só é
            // "passado" para quem olha o calendário errado.
            FusoHorario lisboa = FusoHorario.of("Europe/Lisbon");
            Sala salaEmLisboa = new Sala(SALA_ID, "Sala Lisboa", new Capacidade(10), lisboa, true);
            getSalaPort = id -> Result.of(salaEmLisboa);
            getDataHoraAtualPort = fuso -> Result.of(new DataHora(
                    LocalDateTime.parse("2026-09-15T23:00").atZone(SAO_PAULO.value()).toInstant(), fuso));

            ConsultarDisponibilidadeCommand command =
                    new ConsultarDisponibilidadeCommand(SALA_UUID, LocalDate.of(2026, 9, 16));

            assertDoesNotThrow(() -> useCase().execute(command));
        }

        @Test
        void naoConsultaAAgendaQuandoADataEstaNoPassado() {
            List<SalaId> consultas = new ArrayList<>();
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> {
                consultas.add(id);
                return Result.of(List.of());
            };
            ConsultarDisponibilidadeCommand command =
                    new ConsultarDisponibilidadeCommand(SALA_UUID, DIA.minusDays(1));

            assertThrows(DataNoPassadoException.class, () -> useCase().execute(command));
            assertTrue(consultas.isEmpty());
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

            assertEquals(CAUSA, excecao.getCause());
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
        void agendaIndisponivel() {
            getReservasAtivasDaSalaPort = (id, inicio, fim) -> Result.failure(CAUSA);

            GetReservasAtivasDaSalaUnavailableException excecao = assertThrows(
                    GetReservasAtivasDaSalaUnavailableException.class, () -> useCase().execute(comando()));

            assertEquals(CAUSA, excecao.getCause());
        }
    }
}
