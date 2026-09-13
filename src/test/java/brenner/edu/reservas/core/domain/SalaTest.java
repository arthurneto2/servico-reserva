package brenner.edu.reservas.core.domain;

import brenner.edu.reservas.core.domain.exceptions.AntecedenciaMinimaException;
import brenner.edu.reservas.core.domain.exceptions.CapacidadeExcedidaException;
import brenner.edu.reservas.core.domain.exceptions.ConflitoDeHorarioException;
import brenner.edu.reservas.core.domain.exceptions.FusoIncompativelException;
import brenner.edu.reservas.core.domain.exceptions.SalaInativaException;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.JanelaLivre;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalaTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final FusoHorario LISBOA = FusoHorario.of("Europe/Lisbon");
    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);
    private static final SalaId SALA_ID = SalaId.novo();
    private static final Email ORGANIZADOR = new Email("organizador@brenner.edu");
    private static final QuantidadeParticipantes QUATRO = new QuantidadeParticipantes(4);

    private static final Sala SALA = new Sala(SALA_ID, "Auditório", new Capacidade(10), SAO_PAULO, true);
    private static final Sala SALA_INATIVA =
            new Sala(SALA_ID, "Auditório", new Capacidade(10), SAO_PAULO, false);

    private static DataHora em(LocalDate dia, String hora) {
        LocalDateTime local = LocalDateTime.of(dia, LocalTime.parse(hora));
        return new DataHora(local.atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static DataHora em(String hora) {
        return em(DIA, hora);
    }

    private static Periodo periodo(String inicio, String fim) {
        return new Periodo(em(inicio), em(fim));
    }

    private static Periodo periodoEmLisboa(String inicio, String fim) {
        return new Periodo(emLisboa(inicio), emLisboa(fim));
    }

    private static DataHora emLisboa(String hora) {
        LocalDateTime local = LocalDateTime.of(DIA, LocalTime.parse(hora));
        return new DataHora(local.atZone(LISBOA.value()).toInstant(), LISBOA);
    }

    private static JanelaLivre janela(String inicio, String fim) {
        return new JanelaLivre(em(inicio), em(fim));
    }

    private static Reserva reserva(StatusReserva status, Periodo periodo) {
        return Reserva.reconstituir(ReservaId.novo(), SALA_ID, ORGANIZADOR, periodo,
                QUATRO, status, em("08:00"));
    }

    private static Reserva ativa(String inicio, String fim) {
        return reserva(StatusReserva.CONFIRMADA, periodo(inicio, fim));
    }

    @Nested
    @DisplayName("reservar")
    class Reservar {

        @Test
        void devolveReservaPendenteCriadaAgora() {
            DataHora agora = em("08:00");

            Reserva reserva = SALA.reservar(ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, List.of(), agora);

            assertAll(
                    () -> assertEquals(StatusReserva.PENDENTE, reserva.status()),
                    () -> assertEquals(agora, reserva.criadaEm()),
                    () -> assertEquals(SALA_ID, reserva.salaId()),
                    () -> assertEquals(ORGANIZADOR, reserva.organizador()),
                    () -> assertEquals(periodo("09:00", "10:00"), reserva.periodo()),
                    () -> assertEquals(QUATRO, reserva.participantes()));
        }

        @Test
        void geraUmIdentificadorNovoACadaReserva() {
            DataHora agora = em("08:00");

            Reserva primeira = SALA.reservar(ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, List.of(), agora);
            Reserva segunda = SALA.reservar(ORGANIZADOR, periodo("11:00", "12:00"), QUATRO, List.of(), agora);

            assertNotEquals(primeira.id(), segunda.id());
        }

        @Test
        void rejeitaSalaInativa() {
            assertThrows(SalaInativaException.class, () -> SALA_INATIVA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, List.of(), em("08:00")));
        }

        @Test
        void rejeitaPeriodoEmOutroFuso() {
            // 14:00–15:00 em Lisboa é 11:00–12:00 em São Paulo: dentro do expediente de Lisboa,
            // mas o período não pertence ao fuso desta sala.
            assertThrows(FusoIncompativelException.class, () -> SALA.reservar(
                    ORGANIZADOR, periodoEmLisboa("14:00", "15:00"), QUATRO, List.of(), em("08:00")));
        }

        @Test
        void rejeitaPeriodoQueSoEValidoNoFusoDeOrigem() {
            // 09:00–10:00 em Lisboa é 06:00–07:00 em São Paulo: antes da abertura desta sala.
            assertThrows(FusoIncompativelException.class, () -> SALA.reservar(
                    ORGANIZADOR, periodoEmLisboa("09:00", "10:00"), QUATRO, List.of(),
                    new DataHora(LocalDateTime.of(DIA, LocalTime.parse("04:00"))
                            .atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO)));
        }

        @Test
        void rejeitaParticipantesAcimaDaCapacidade() {
            assertThrows(CapacidadeExcedidaException.class, () -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), new QuantidadeParticipantes(11),
                    List.of(), em("08:00")));
        }

        @Test
        void aceitaParticipantesExatamenteNaCapacidade() {
            assertDoesNotThrow(() -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), new QuantidadeParticipantes(10),
                    List.of(), em("08:00")));
        }

        @Test
        void aceitaAntecedenciaDeExatamenteUmaHora() {
            assertDoesNotThrow(() -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, List.of(), em("08:00")));
        }

        @Test
        void rejeitaAntecedenciaMenorQueUmaHora() {
            assertThrows(AntecedenciaMinimaException.class, () -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, List.of(), em("08:01")));
        }

        @Test
        void rejeitaPeriodoQueJaComecou() {
            assertThrows(AntecedenciaMinimaException.class, () -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, List.of(), em("09:30")));
        }

        @Test
        void rejeitaPeriodoQueSobrepoeReservaAtiva() {
            List<Reserva> reservasAtivas = List.of(ativa("09:30", "10:30"));

            assertThrows(ConflitoDeHorarioException.class, () -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, reservasAtivas, em("08:00")));
        }

        @Test
        void aceitaPeriodoQueApenasEncostaEmReservaAtiva() {
            List<Reserva> reservasAtivas = List.of(ativa("10:00", "11:00"), ativa("08:00", "09:00"));

            assertDoesNotThrow(() -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, reservasAtivas, em("08:00")));
        }

        @Test
        void verificaConflitoContraTodasAsReservasDaLista() {
            List<Reserva> reservasAtivas = List.of(
                    ativa("08:00", "09:00"),
                    ativa("12:00", "13:00"),
                    ativa("09:30", "10:30"));

            assertThrows(ConflitoDeHorarioException.class, () -> SALA.reservar(
                    ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, reservasAtivas, em("08:00")));
        }

        @Nested
        @DisplayName("precedência entre as regras")
        class Precedencia {

            @Test
            void salaInativaVenceCapacidade() {
                assertThrows(SalaInativaException.class, () -> SALA_INATIVA.reservar(
                        ORGANIZADOR, periodo("09:00", "10:00"), new QuantidadeParticipantes(99),
                        List.of(), em("08:00")));
            }

            @Test
            void salaInativaVenceFusoIncompativel() {
                assertThrows(SalaInativaException.class, () -> SALA_INATIVA.reservar(
                        ORGANIZADOR, periodoEmLisboa("14:00", "15:00"), QUATRO,
                        List.of(), em("08:00")));
            }

            @Test
            void fusoIncompativelVenceCapacidade() {
                assertThrows(FusoIncompativelException.class, () -> SALA.reservar(
                        ORGANIZADOR, periodoEmLisboa("14:00", "15:00"),
                        new QuantidadeParticipantes(99), List.of(), em("08:00")));
            }

            @Test
            void capacidadeVenceAntecedencia() {
                assertThrows(CapacidadeExcedidaException.class, () -> SALA.reservar(
                        ORGANIZADOR, periodo("09:00", "10:00"), new QuantidadeParticipantes(99),
                        List.of(), em("08:59")));
            }

            @Test
            void antecedenciaVenceConflito() {
                List<Reserva> reservasAtivas = List.of(ativa("09:30", "10:30"));

                assertThrows(AntecedenciaMinimaException.class, () -> SALA.reservar(
                        ORGANIZADOR, periodo("09:00", "10:00"), QUATRO, reservasAtivas, em("08:59")));
            }
        }
    }

    @Nested
    @DisplayName("horariosLivresEm")
    class HorariosLivres {

        @Test
        void diaSemReservasTemUmaJanelaInteira() {
            assertEquals(List.of(janela("08:00", "20:00")), SALA.horariosLivresEm(DIA, List.of()));
        }

        @Test
        void descontaUmaReservaNoMeioDoDia() {
            List<Reserva> reservas = List.of(ativa("10:00", "12:00"));

            assertEquals(
                    List.of(janela("08:00", "10:00"), janela("12:00", "20:00")),
                    SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void naoGeraJanelaVaziaQuandoAReservaComecaNaAbertura() {
            List<Reserva> reservas = List.of(ativa("08:00", "10:00"));

            assertEquals(List.of(janela("10:00", "20:00")), SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void naoGeraJanelaVaziaQuandoAReservaTerminaNoFechamento() {
            List<Reserva> reservas = List.of(ativa("18:00", "20:00"));

            assertEquals(List.of(janela("08:00", "18:00")), SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void diaTotalmenteOcupadoNaoTemJanelas() {
            List<Reserva> reservas = List.of(
                    ativa("08:00", "12:00"),
                    ativa("12:00", "16:00"),
                    ativa("16:00", "20:00"));

            assertEquals(List.of(), SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void ordenaAsJanelasIndependenteDaOrdemDaLista() {
            List<Reserva> reservas = List.of(
                    ativa("16:00", "17:00"),
                    ativa("09:00", "10:00"),
                    ativa("12:00", "13:00"));

            assertEquals(
                    List.of(janela("08:00", "09:00"),
                            janela("10:00", "12:00"),
                            janela("13:00", "16:00"),
                            janela("17:00", "20:00")),
                    SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void fundeReservasSobrepostas() {
            List<Reserva> reservas = List.of(
                    ativa("10:00", "12:00"),
                    ativa("11:00", "13:00"));

            assertEquals(
                    List.of(janela("08:00", "10:00"), janela("13:00", "20:00")),
                    SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void fundeReservaContidaEmOutra() {
            List<Reserva> reservas = List.of(
                    ativa("10:00", "14:00"),
                    ativa("11:00", "12:00"));

            assertEquals(
                    List.of(janela("08:00", "10:00"), janela("14:00", "20:00")),
                    SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void consideraPendenteEConfirmada() {
            List<Reserva> reservas = List.of(
                    reserva(StatusReserva.PENDENTE, periodo("10:00", "11:00")),
                    reserva(StatusReserva.CONFIRMADA, periodo("14:00", "15:00")));

            assertEquals(
                    List.of(janela("08:00", "10:00"),
                            janela("11:00", "14:00"),
                            janela("15:00", "20:00")),
                    SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void ignoraCanceladaEExpirada() {
            List<Reserva> reservas = List.of(
                    reserva(StatusReserva.CANCELADA, periodo("10:00", "12:00")),
                    reserva(StatusReserva.EXPIRADA, periodo("14:00", "16:00")));

            assertEquals(List.of(janela("08:00", "20:00")), SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void ignoraReservaDeOutroDia() {
            Periodo outroDia = new Periodo(em(DIA.plusDays(1), "10:00"), em(DIA.plusDays(1), "12:00"));
            List<Reserva> reservas = List.of(reserva(StatusReserva.CONFIRMADA, outroDia));

            assertEquals(List.of(janela("08:00", "20:00")), SALA.horariosLivresEm(DIA, reservas));
        }

        @Test
        void devolveListaImutavel() {
            List<JanelaLivre> livres = SALA.horariosLivresEm(DIA, List.of());

            assertThrows(UnsupportedOperationException.class,
                    () -> livres.add(janela("08:00", "09:00")));
        }

        @Test
        void asJanelasUsamOFusoDaSala() {
            List<JanelaLivre> livres = SALA.horariosLivresEm(DIA, List.of(ativa("10:00", "12:00")));

            assertTrue(livres.stream().allMatch(janela ->
                    janela.inicio().fusoHorario().equals(SAO_PAULO)
                            && janela.fim().fusoHorario().equals(SAO_PAULO)));
        }
    }
}
