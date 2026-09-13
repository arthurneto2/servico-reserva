package brenner.edu.reservas.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de {@code POST /reservas} e de {@code POST /reservas/{id}/check-in}.
 *
 * <p>As datas são locais ao fuso da sala e vão sem segundos e sem offset; o fuso viaja em campo
 * próprio, e é ele que dá sentido aos horários. Sem esse par, "14:00" não diz nada sobre qual
 * instante a reserva ocupa.
 */
@Schema(description = "Reserva")
public record ReservaResponse(

        UUID id,

        UUID salaId,

        @Schema(example = "ana@empresa.com")
        String organizador,

        @Schema(example = "2026-09-15T14:00", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime inicio,

        @Schema(example = "2026-09-15T15:30", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime fim,

        @Schema(description = "Fuso da sala, que dá sentido aos horários", example = "America/Sao_Paulo")
        String fuso,

        int quantidadeParticipantes,

        @Schema(example = "PENDENTE", allowableValues = {"PENDENTE", "CONFIRMADA", "CANCELADA", "EXPIRADA"})
        String status,

        @Schema(example = "2026-09-12T09:03", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime criadaEm) {
}
