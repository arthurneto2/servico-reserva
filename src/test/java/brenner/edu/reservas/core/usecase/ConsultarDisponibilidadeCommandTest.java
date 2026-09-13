package brenner.edu.reservas.core.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarDisponibilidadeCommandTest {

    private static final UUID SALA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate DATA = LocalDate.of(2026, 9, 15);

    @Test
    void aceitaComandoCompleto() {
        ConsultarDisponibilidadeCommand command = new ConsultarDisponibilidadeCommand(SALA, DATA);

        assertAll(
                () -> assertEquals(SALA, command.salaId()),
                () -> assertEquals(DATA, command.data()));
    }

    @Nested
    @DisplayName("campos obrigatórios")
    class Obrigatorios {

        @Test
        void rejeitaSalaIdNula() {
            assertThrows(NullPointerException.class,
                    () -> new ConsultarDisponibilidadeCommand(null, DATA));
        }

        @Test
        void rejeitaDataNula() {
            assertThrows(NullPointerException.class,
                    () -> new ConsultarDisponibilidadeCommand(SALA, null));
        }
    }

    @Test
    @DisplayName("não valida data no passado: só o UseCase conhece o 'agora' do fuso da sala")
    void aceitaDataNoPassado() {
        assertDoesNotThrow(
                () -> new ConsultarDisponibilidadeCommand(SALA, LocalDate.of(1999, 1, 1)));
    }
}
