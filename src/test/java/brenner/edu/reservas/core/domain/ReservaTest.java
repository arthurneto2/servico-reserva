package brenner.edu.reservas.core.domain;

import brenner.edu.reservas.core.domain.exceptions.CheckInAntecipadoException;
import brenner.edu.reservas.core.domain.exceptions.PrazoDeCancelamentoExpiradoException;
import brenner.edu.reservas.core.domain.exceptions.ReservaExpiradaException;
import brenner.edu.reservas.core.domain.exceptions.ReservaInvalidaException;
import brenner.edu.reservas.core.domain.exceptions.SolicitanteNaoEOrganizadorException;
import brenner.edu.reservas.core.domain.exceptions.TransicaoDeStatusInvalidaException;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservaTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final SalaId SALA_ID = SalaId.novo();
    private static final Email ORGANIZADOR = new Email("ana@empresa.com");
    private static final Periodo PERIODO = new Periodo(em("2026-09-15T14:00"), em("2026-09-15T15:30"));

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static Reserva reservaCom(StatusReserva status) {
        return Reserva.reconstituir(ReservaId.novo(), SALA_ID, ORGANIZADOR, PERIODO,
                new QuantidadeParticipantes(4), status, em("2026-09-14T10:00"));
    }

    @Nested
    @DisplayName("cancelar")
    class Cancelar {

        @Test
        void cancelaReservaPendente() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            reserva.cancelar(ORGANIZADOR, em("2026-09-15T09:00"));

            assertEquals(StatusReserva.CANCELADA, reserva.status());
        }

        @Test
        void cancelaReservaConfirmada() {
            Reserva reserva = reservaCom(StatusReserva.CONFIRMADA);

            reserva.cancelar(ORGANIZADOR, em("2026-09-15T09:00"));

            assertEquals(StatusReserva.CANCELADA, reserva.status());
        }

        @Test
        void reservaCanceladaDeixaDeOcuparAAgenda() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            reserva.cancelar(ORGANIZADOR, em("2026-09-15T09:00"));

            assertFalse(reserva.ocupaAgenda());
        }

        @Test
        void reconheceOOrganizadorIndependenteDaCaixa() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertDoesNotThrow(() -> reserva.cancelar(new Email("ANA@Empresa.com"), em("2026-09-15T09:00")));
        }
    }

    @Nested
    @DisplayName("cancelar: status que não admite cancelamento")
    class StatusInvalido {

        @Test
        void recusaReservaJaCancelada() {
            Reserva reserva = reservaCom(StatusReserva.CANCELADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.cancelar(ORGANIZADOR, em("2026-09-15T09:00")));
        }

        @Test
        void recusaReservaExpirada() {
            Reserva reserva = reservaCom(StatusReserva.EXPIRADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.cancelar(ORGANIZADOR, em("2026-09-15T09:00")));
        }

        @Test
        void mantemOStatusQuandoRecusa() {
            Reserva reserva = reservaCom(StatusReserva.EXPIRADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.cancelar(ORGANIZADOR, em("2026-09-15T09:00")));
            assertEquals(StatusReserva.EXPIRADA, reserva.status());
        }
    }

    @Nested
    @DisplayName("cancelar: só o organizador cancela")
    class SolicitanteInvalido {

        @Test
        void recusaSolicitanteQueNaoOrganizou() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(SolicitanteNaoEOrganizadorException.class,
                    () -> reserva.cancelar(new Email("bruno@empresa.com"), em("2026-09-15T09:00")));
        }

        @Test
        void mantemOStatusQuandoRecusa() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(SolicitanteNaoEOrganizadorException.class,
                    () -> reserva.cancelar(new Email("bruno@empresa.com"), em("2026-09-15T09:00")));
            assertEquals(StatusReserva.PENDENTE, reserva.status());
        }
    }

    @Nested
    @DisplayName("cancelar: prazo de 2 horas antes do início")
    class PrazoDeCancelamento {

        @Test
        void aceitaExatamenteDuasHorasAntes() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            reserva.cancelar(ORGANIZADOR, em("2026-09-15T12:00"));

            assertEquals(StatusReserva.CANCELADA, reserva.status());
        }

        @Test
        void recusaUmMinutoDepoisDoPrazo() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(PrazoDeCancelamentoExpiradoException.class,
                    () -> reserva.cancelar(ORGANIZADOR, em("2026-09-15T12:01")));
        }

        @Test
        void recusaDepoisDoInicioDaReserva() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(PrazoDeCancelamentoExpiradoException.class,
                    () -> reserva.cancelar(ORGANIZADOR, em("2026-09-15T14:30")));
        }
    }

    @Nested
    @DisplayName("cancelar: precedência entre as regras")
    class Precedencia {

        @Test
        void statusInvalidoPrecedeSolicitanteErrado() {
            Reserva reserva = reservaCom(StatusReserva.CANCELADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.cancelar(new Email("bruno@empresa.com"), em("2026-09-15T09:00")));
        }

        @Test
        void solicitanteErradoPrecedePrazoExpirado() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(SolicitanteNaoEOrganizadorException.class,
                    () -> reserva.cancelar(new Email("bruno@empresa.com"), em("2026-09-15T13:59")));
        }
    }

    @Nested
    @DisplayName("cancelar: argumentos obrigatórios")
    class Obrigatorios {

        @Test
        void recusaSolicitanteNulo() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(ReservaInvalidaException.class,
                    () -> reserva.cancelar(null, em("2026-09-15T09:00")));
        }

        @Test
        void recusaAgoraNulo() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(ReservaInvalidaException.class, () -> reserva.cancelar(ORGANIZADOR, null));
        }
    }

    @Nested
    @DisplayName("realizarCheckIn")
    class RealizarCheckIn {

        @Test
        void confirmaNaJanelaDeCheckIn() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            reserva.realizarCheckIn(em("2026-09-15T14:00"));

            assertEquals(StatusReserva.CONFIRMADA, reserva.status());
        }

        @Test
        void aceitaExatamenteDezMinutosAntesDoInicio() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            reserva.realizarCheckIn(em("2026-09-15T13:50"));

            assertEquals(StatusReserva.CONFIRMADA, reserva.status());
        }

        @Test
        void aceitaExatamenteQuinzeMinutosDepoisDoInicio() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            reserva.realizarCheckIn(em("2026-09-15T14:15"));

            assertEquals(StatusReserva.CONFIRMADA, reserva.status());
        }

        @Test
        void reservaConfirmadaContinuaOcupandoAAgenda() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            reserva.realizarCheckIn(em("2026-09-15T14:00"));

            assertTrue(reserva.ocupaAgenda());
        }
    }

    @Nested
    @DisplayName("realizarCheckIn: antes da janela")
    class CheckInAntecipado {

        @Test
        void recusaUmMinutoAntesDaJanela() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(CheckInAntecipadoException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T13:49")));
        }

        @Test
        void mantemPendenteQuandoRecusa() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(CheckInAntecipadoException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T09:00")));
            assertEquals(StatusReserva.PENDENTE, reserva.status());
        }
    }

    @Nested
    @DisplayName("realizarCheckIn: depois da janela expira a reserva")
    class CheckInExpirado {

        @Test
        void recusaUmMinutoDepoisDaJanela() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(ReservaExpiradaException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T14:16")));
        }

        @Test
        void expiraAReservaAlemDeLancar() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(ReservaExpiradaException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T14:16")));
            assertEquals(StatusReserva.EXPIRADA, reserva.status());
        }

        @Test
        void reservaExpiradaLiberaAAgenda() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(ReservaExpiradaException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T18:00")));
            assertFalse(reserva.ocupaAgenda());
        }
    }

    @Nested
    @DisplayName("realizarCheckIn: status que não admite check-in")
    class CheckInComStatusInvalido {

        @Test
        void recusaReservaJaConfirmada() {
            Reserva reserva = reservaCom(StatusReserva.CONFIRMADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T14:00")));
        }

        @Test
        void recusaReservaCancelada() {
            Reserva reserva = reservaCom(StatusReserva.CANCELADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T14:00")));
        }

        @Test
        void recusaReservaJaExpirada() {
            Reserva reserva = reservaCom(StatusReserva.EXPIRADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T14:00")));
        }

        @Test
        void statusInvalidoPrecedeJanelaDeCheckIn() {
            Reserva reserva = reservaCom(StatusReserva.CANCELADA);

            assertThrows(TransicaoDeStatusInvalidaException.class,
                    () -> reserva.realizarCheckIn(em("2026-09-15T20:00")));
        }
    }

    @Nested
    @DisplayName("realizarCheckIn: argumentos obrigatórios")
    class CheckInObrigatorios {

        @Test
        void recusaAgoraNulo() {
            Reserva reserva = reservaCom(StatusReserva.PENDENTE);

            assertThrows(ReservaInvalidaException.class, () -> reserva.realizarCheckIn(null));
        }
    }

    @Nested
    @DisplayName("ocupaAgenda")
    class OcupaAgenda {

        @Test
        void pendenteOcupa() {
            assertTrue(reservaCom(StatusReserva.PENDENTE).ocupaAgenda());
        }

        @Test
        void confirmadaOcupa() {
            assertTrue(reservaCom(StatusReserva.CONFIRMADA).ocupaAgenda());
        }

        @Test
        void canceladaNaoOcupa() {
            assertFalse(reservaCom(StatusReserva.CANCELADA).ocupaAgenda());
        }

        @Test
        void expiradaNaoOcupa() {
            assertFalse(reservaCom(StatusReserva.EXPIRADA).ocupaAgenda());
        }
    }
}
