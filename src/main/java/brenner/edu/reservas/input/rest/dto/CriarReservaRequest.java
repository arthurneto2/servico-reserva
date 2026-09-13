package brenner.edu.reservas.input.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Corpo de {@code POST /reservas}.
 *
 * <p>A Bean Validation daqui cobre <b>apenas forma</b> — campo presente, texto não vazio, número
 * positivo — e o que ela rejeita vira <b>400</b>. Formato de e-mail, ordem dos horários, duração,
 * alinhamento de 15 minutos e horário de funcionamento passam batido de propósito: são regras de
 * negócio que morrem no Value Object ou no agregado, e resultam em <b>422</b>.
 *
 * <p>{@code inicio} e {@code fim} são horários <b>locais ao fuso da sala</b>, sem offset: o
 * cliente não escolhe fuso, ele é o da sala reservada.
 */
@Schema(description = "Pedido de reserva de uma sala")
public record CriarReservaRequest(

        @Schema(example = "11111111-1111-1111-1111-111111111111")
        @NotNull(message = "salaId é obrigatório")
        UUID salaId,

        @Schema(example = "ana@empresa.com")
        @NotBlank(message = "organizador é obrigatório")
        String organizador,

        @Schema(description = "Início, local ao fuso da sala", example = "2026-09-15T14:00", type = "string")
        @NotNull(message = "inicio é obrigatório")
        LocalDateTime inicio,

        @Schema(description = "Fim, local ao fuso da sala", example = "2026-09-15T15:30", type = "string")
        @NotNull(message = "fim é obrigatório")
        LocalDateTime fim,

        @Schema(example = "6")
        @Positive(message = "quantidadeParticipantes deve ser > 0")
        int quantidadeParticipantes) {
}
