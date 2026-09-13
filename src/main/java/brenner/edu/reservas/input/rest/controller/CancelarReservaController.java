package brenner.edu.reservas.input.rest.controller;

import brenner.edu.reservas.core.usecase.CancelarReservaUseCase;
import brenner.edu.reservas.core.usecase.commands.CancelarReservaCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * {@code DELETE /reservas/{id}}.
 *
 * <p>O solicitante vem no header {@code X-Solicitante} porque um DELETE não tem corpo. Quem
 * confere se ele é o organizador é o agregado {@code Reserva}, não este controller: aqui o header
 * é apenas transportado para dentro do Command.
 */
@RestController
@RequestMapping("/reservas")
@Tag(name = "Reservas")
public class CancelarReservaController {

    private final CancelarReservaUseCase useCase;

    public CancelarReservaController(CancelarReservaUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Cancela uma reserva",
            description = "Só o organizador cancela, e só até 2 horas antes do início.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva cancelada"),
            @ApiResponse(responseCode = "400", description = "Header X-Solicitante ausente ou id inválido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada", content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Status não admite cancelamento, solicitante não é o organizador ou "
                            + "prazo de cancelamento expirado",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Dependência externa indisponível",
                    content = @Content)})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @Parameter(description = "Id da reserva") @PathVariable UUID id,
            @Parameter(description = "E-mail de quem pede o cancelamento", example = "ana@empresa.com")
            @RequestHeader("X-Solicitante") String solicitante) {

        useCase.execute(new CancelarReservaCommand(id, solicitante));
        return ResponseEntity.noContent().build();
    }
}
