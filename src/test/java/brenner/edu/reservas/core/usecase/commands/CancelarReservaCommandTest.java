package brenner.edu.reservas.core.usecase.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelarReservaCommandTest {

    private static final UUID RESERVA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String SOLICITANTE = "ana@empresa.com";

    @Test
    void aceitaComandoCompleto() {
        CancelarReservaCommand command = new CancelarReservaCommand(RESERVA, SOLICITANTE);

        assertAll(
                () -> assertEquals(RESERVA, command.reservaId()),
                () -> assertEquals(SOLICITANTE, command.solicitante()));
    }

    @Nested
    @DisplayName("campos obrigatórios")
    class Obrigatorios {

        @Test
        void rejeitaReservaIdNula() {
            assertThrows(NullPointerException.class,
                    () -> new CancelarReservaCommand(null, SOLICITANTE));
        }

        @Test
        void rejeitaSolicitanteNulo() {
            assertThrows(NullPointerException.class,
                    () -> new CancelarReservaCommand(RESERVA, null));
        }
    }

    @Test
    @DisplayName("não valida o formato do solicitante: quem faz isso é o VO Email")
    void aceitaSolicitanteQueNaoParecerEmail() {
        assertDoesNotThrow(() -> new CancelarReservaCommand(RESERVA, "banana"));
    }
}
