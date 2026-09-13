package brenner.edu.reservas.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A reserva como ela aparece dentro da disponibilidade: só o que explica por que uma faixa do dia
 * não está livre. Organizador e quantidade de participantes ficam de fora — quem consulta a agenda
 * de uma sala não precisa saber quem reservou.
 */
@Schema(description = "Reserva que ocupa a agenda no dia consultado")
public record ReservaResumoResponse(

        UUID id,

        @Schema(example = "2026-09-15T14:00", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime inicio,

        @Schema(example = "2026-09-15T15:30", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime fim,

        @Schema(example = "PENDENTE")
        String status) {
}
