package brenner.edu.reservas.core.usecase;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RealizarCheckInCommandTest {

    private static final UUID RESERVA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void aceitaComandoCompleto() {
        RealizarCheckInCommand command = new RealizarCheckInCommand(RESERVA);

        assertEquals(RESERVA, command.reservaId());
    }

    @Test
    void rejeitaReservaIdNula() {
        assertThrows(NullPointerException.class, () -> new RealizarCheckInCommand(null));
    }
}
