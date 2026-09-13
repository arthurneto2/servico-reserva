package brenner.edu.reservas.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Uma lacuna livre na agenda. O fuso não se repete aqui: ele está uma vez na resposta que a contém. */
@Schema(description = "Intervalo livre na agenda da sala")
public record JanelaLivreResponse(

        @Schema(example = "2026-09-15T08:00", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime inicio,

        @Schema(example = "2026-09-15T14:00", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime fim) {
}
