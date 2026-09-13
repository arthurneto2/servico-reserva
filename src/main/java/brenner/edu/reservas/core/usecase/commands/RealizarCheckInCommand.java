package brenner.edu.reservas.core.usecase.commands;

import java.util.Objects;
import java.util.UUID;

/**
 * Entrada do {@code RealizarCheckInUseCase}.
 *
 * <p>O endpoint não tem corpo nem header extra, então o Command carrega só o id da reserva. Ele
 * existe mesmo assim porque o padrão é invariável: todo UseCase expõe {@code execute(XxxCommand)},
 * e um Command de um campo hoje é o lugar natural para o segundo campo de amanhã.
 *
 * <p>O "agora" do check-in não vem daqui: é o {@code GetDataHoraAtualPort} que o fornece, no fuso
 * da sala. Aceitá-lo como parâmetro deixaria o cliente escolher a hora e burlar a janela de
 * check-in.
 */
public record RealizarCheckInCommand(UUID reservaId) {

    public RealizarCheckInCommand {
        Objects.requireNonNull(reservaId, "reservaId é obrigatório");
    }
}
