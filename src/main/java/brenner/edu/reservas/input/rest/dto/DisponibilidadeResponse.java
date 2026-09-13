package brenner.edu.reservas.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resposta de {@code GET /salas/{id}/disponibilidade}.
 *
 * <p>Devolve as duas faces do mesmo dia: o que está livre e o que o ocupa. As reservas vêm junto
 * porque uma lacuna sozinha não explica a agenda — quem vê "15:30 às 20:00 livre" costuma querer
 * saber o que acontece antes disso.
 */
@Schema(description = "Agenda de uma sala em um dia")
public record DisponibilidadeResponse(

        UUID salaId,

        @Schema(example = "Sala Pirapora")
        String nome,

        @Schema(example = "America/Sao_Paulo")
        String fuso,

        @Schema(example = "2026-09-15", type = "string")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate data,

        List<JanelaLivreResponse> horariosLivres,

        List<ReservaResumoResponse> reservas) {
}
