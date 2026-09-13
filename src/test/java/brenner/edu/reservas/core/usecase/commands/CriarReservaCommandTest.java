package brenner.edu.reservas.core.usecase.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CriarReservaCommandTest {

    private static final UUID SALA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String ORGANIZADOR = "ana@empresa.com";
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 9, 15, 14, 0);
    private static final LocalDateTime FIM = LocalDateTime.of(2026, 9, 15, 15, 30);

    private static CriarReservaCommand comParticipantes(int quantidade) {
        return new CriarReservaCommand(SALA, ORGANIZADOR, INICIO, FIM, quantidade);
    }

    @Test
    void aceitaComandoCompleto() {
        CriarReservaCommand command = comParticipantes(6);

        assertAll(
                () -> assertEquals(SALA, command.salaId()),
                () -> assertEquals(ORGANIZADOR, command.organizador()),
                () -> assertEquals(INICIO, command.inicio()),
                () -> assertEquals(FIM, command.fim()),
                () -> assertEquals(6, command.quantidadeParticipantes()));
    }

    @Nested
    @DisplayName("campos obrigatórios")
    class Obrigatorios {

        @Test
        void rejeitaSalaIdNula() {
            assertThrows(NullPointerException.class,
                    () -> new CriarReservaCommand(null, ORGANIZADOR, INICIO, FIM, 6));
        }

        @Test
        void rejeitaOrganizadorNulo() {
            assertThrows(NullPointerException.class,
                    () -> new CriarReservaCommand(SALA, null, INICIO, FIM, 6));
        }

        @Test
        void rejeitaInicioNulo() {
            assertThrows(NullPointerException.class,
                    () -> new CriarReservaCommand(SALA, ORGANIZADOR, null, FIM, 6));
        }

        @Test
        void rejeitaFimNulo() {
            assertThrows(NullPointerException.class,
                    () -> new CriarReservaCommand(SALA, ORGANIZADOR, INICIO, null, 6));
        }
    }

    @Nested
    @DisplayName("quantidade de participantes positiva")
    class Quantidade {

        @Test
        void rejeitaZero() {
            assertThrows(IllegalArgumentException.class, () -> comParticipantes(0));
        }

        @Test
        void rejeitaNegativa() {
            assertThrows(IllegalArgumentException.class, () -> comParticipantes(-1));
        }

        @Test
        void aceitaUm() {
            assertDoesNotThrow(() -> comParticipantes(1));
        }
    }

    @Nested
    @DisplayName("o Command valida forma, nunca regra de negócio")
    class SomenteForma {

        @Test
        void aceitaFimAntesDoInicio() {
            assertDoesNotThrow(() -> new CriarReservaCommand(SALA, ORGANIZADOR, FIM, INICIO, 6));
        }

        @Test
        void aceitaOrganizadorQueNaoParecerEmail() {
            assertDoesNotThrow(() -> new CriarReservaCommand(SALA, "banana", INICIO, FIM, 6));
        }

        @Test
        void aceitaPeriodoForaDoHorarioDeFuncionamento() {
            LocalDateTime madrugadaInicio = LocalDateTime.of(2026, 9, 15, 3, 0);
            LocalDateTime madrugadaFim = LocalDateTime.of(2026, 9, 15, 4, 0);

            assertDoesNotThrow(
                    () -> new CriarReservaCommand(SALA, ORGANIZADOR, madrugadaInicio, madrugadaFim, 6));
        }
    }
}
