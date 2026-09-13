package brenner.edu.reservas.core.usecase.commands;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrada do {@code CriarReservaUseCase}.
 *
 * <p>{@code inicio} e {@code fim} são horários <b>locais ao fuso da sala</b>, sem offset: é o
 * UseCase que, de posse da {@code Sala}, os converte em {@code DataHora}. Por isso o Command
 * carrega {@link LocalDateTime} e não {@code Instant} — o controller não sabe em que fuso a sala
 * vive.
 *
 * <p>A validação aqui é <b>apenas de forma</b> (não nulo, número positivo) e resulta em 400.
 * Ordem dos horários, duração, alinhamento, horário de funcionamento e formato do e-mail são
 * regras de negócio: nascem dos Value Objects e do agregado, e resultam em 422.
 */
public record CriarReservaCommand(UUID salaId,
                                  String organizador,
                                  LocalDateTime inicio,
                                  LocalDateTime fim,
                                  int quantidadeParticipantes) {

    public CriarReservaCommand {
        Objects.requireNonNull(salaId, "salaId é obrigatório");
        Objects.requireNonNull(organizador, "organizador é obrigatório");
        Objects.requireNonNull(inicio, "inicio é obrigatório");
        Objects.requireNonNull(fim, "fim é obrigatório");
        if (quantidadeParticipantes <= 0)
            throw new IllegalArgumentException("quantidadeParticipantes deve ser > 0");
    }
}
