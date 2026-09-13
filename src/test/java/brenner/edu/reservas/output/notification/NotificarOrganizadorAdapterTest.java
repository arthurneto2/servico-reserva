package brenner.edu.reservas.output.notification;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.shared.Result;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificarOrganizadorAdapterTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");

    private final NotificarOrganizadorAdapter adapter = new NotificarOrganizadorAdapter();

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static Reserva reserva() {
        return Reserva.reconstituir(ReservaId.novo(), SalaId.novo(), new Email("ana@empresa.com"),
                new Periodo(em("2026-09-15T14:00"), em("2026-09-15T15:30")),
                new QuantidadeParticipantes(6), StatusReserva.PENDENTE, em("2026-09-14T10:00"));
    }

    @Test
    void notificacaoEnviadaEUmSucessoVazio() {
        // Result<Void> não tem valor a carregar: Result.of(null) é proibido, então o sucesso
        // deste port é necessariamente o estado vazio.
        Result<Void> resultado = adapter.notificar(reserva());

        assertAll(
                () -> assertTrue(resultado.isSuccess()),
                () -> assertTrue(resultado.isEmpty()),
                () -> assertFalse(resultado.isFailure()));
    }

    @Test
    void reservaNulaEFalhaENaoExcecao() {
        Result<Void> resultado = adapter.notificar(null);

        assertTrue(resultado.isFailure());
    }
}
