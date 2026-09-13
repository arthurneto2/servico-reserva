package brenner.edu.reservas.input.rest.controller;

import brenner.edu.reservas.core.usecase.RealizarCheckInUseCase;
import brenner.edu.reservas.core.usecase.commands.RealizarCheckInCommand;
import brenner.edu.reservas.input.rest.dto.ReservaResponse;
import brenner.edu.reservas.input.rest.mapper.ReservaResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * {@code POST /reservas/{id}/check-in}.
 *
 * <p>Sem corpo: o "agora" do check-in vem do port de tempo, no fuso da sala. Aceitá-lo do cliente
 * seria deixá-lo escolher a hora e burlar a janela.
 *
 * <p>Responde 200, e não 201: o check-in confirma uma reserva existente, não cria recurso novo.
 */
@RestController
@RequestMapping("/reservas")
@Tag(name = "Reservas")
public class RealizarCheckInController {

    private final RealizarCheckInUseCase useCase;
    private final ReservaResponseMapper mapper;

    public RealizarCheckInController(RealizarCheckInUseCase useCase, ReservaResponseMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Realiza o check-in de uma reserva",
            description = """
                    Confirma a reserva se o check-in ocorrer entre 10 minutos antes e 15 minutos
                    depois do início. Depois dessa janela a reserva é marcada EXPIRADA — e a
                    expiração fica gravada, embora a resposta seja de erro.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada"),
            @ApiResponse(responseCode = "400", description = "Id inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reserva não encontrada", content = @Content),
            @ApiResponse(responseCode = "422",
                    description = "Status não admite check-in, check-in antecipado ou reserva expirada",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Dependência externa indisponível",
                    content = @Content)})
    @PostMapping("/{id}/check-in")
    public ReservaResponse realizarCheckIn(
            @Parameter(description = "Id da reserva") @PathVariable UUID id) {

        return mapper.toResponse(useCase.execute(new RealizarCheckInCommand(id)));
    }
}
