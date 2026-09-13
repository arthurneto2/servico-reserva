package brenner.edu.reservas.core.usecase;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrada do {@code ConsultarDisponibilidadeUseCase}.
 *
 * <p>{@code data} é o dia local ao fuso da sala cuja agenda será consultada. Data no passado não
 * é rejeitada aqui: "passado" depende do agora do fuso da sala, que só o UseCase obtém — via
 * {@code GetDataHoraAtualPort} — e que resulta em {@code DataNoPassadoException} (422), não 400.
 */
public record ConsultarDisponibilidadeCommand(UUID salaId, LocalDate data) {

    public ConsultarDisponibilidadeCommand {
        Objects.requireNonNull(salaId, "salaId é obrigatório");
        Objects.requireNonNull(data, "data é obrigatória");
    }
}
