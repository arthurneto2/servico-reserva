package brenner.edu.reservas.core.usecase.commands;

import java.util.Objects;
import java.util.UUID;

/**
 * Entrada do {@code CancelarReservaUseCase}.
 *
 * <p>{@code solicitante} vem do header {@code X-Solicitante} e é comparado com o organizador
 * pelo agregado {@code Reserva}: quem não organizou não cancela. O Command apenas garante que o
 * header veio; se o valor não for um e-mail válido, quem reclama é o VO {@code Email} (422).
 */
public record CancelarReservaCommand(UUID reservaId, String solicitante) {

    public CancelarReservaCommand {
        Objects.requireNonNull(reservaId, "reservaId é obrigatório");
        Objects.requireNonNull(solicitante, "solicitante é obrigatório");
    }
}
