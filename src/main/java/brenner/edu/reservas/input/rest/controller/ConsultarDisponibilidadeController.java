package brenner.edu.reservas.input.rest.controller;

import brenner.edu.reservas.core.usecase.ConsultarDisponibilidadeUseCase;
import brenner.edu.reservas.core.usecase.commands.ConsultarDisponibilidadeCommand;
import brenner.edu.reservas.input.rest.dto.DisponibilidadeResponse;
import brenner.edu.reservas.input.rest.mapper.DisponibilidadeResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code GET /salas/{id}/disponibilidade?data=yyyy-MM-dd}.
 *
 * <p>Consulta também é UseCase: mesmo Command, mesmos ports, mesmo tratamento de indisponibilidade.
 * Não há atalho de leitura direto ao repositório.
 */
@RestController
@RequestMapping("/salas")
@Tag(name = "Salas", description = "Consulta da agenda das salas")
public class ConsultarDisponibilidadeController {

    private final ConsultarDisponibilidadeUseCase useCase;
    private final DisponibilidadeResponseMapper mapper;

    public ConsultarDisponibilidadeController(ConsultarDisponibilidadeUseCase useCase,
                                              DisponibilidadeResponseMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Lista os horários livres de uma sala em um dia",
            description = """
                    As lacunas são calculadas entre 08:00 e 20:00 no fuso da sala, descontando as
                    reservas PENDENTE e CONFIRMADA. O dia é local ao fuso da sala.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda do dia"),
            @ApiResponse(responseCode = "400", description = "Data ausente ou em formato inválido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Sala não encontrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Data no passado, no fuso da sala",
                    content = @Content),
            @ApiResponse(responseCode = "503", description = "Dependência externa indisponível",
                    content = @Content)})
    @GetMapping("/{id}/disponibilidade")
    public DisponibilidadeResponse consultar(
            @Parameter(description = "Id da sala") @PathVariable UUID id,
            @Parameter(description = "Dia consultado, local ao fuso da sala", example = "2026-09-15")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        return mapper.toResponse(useCase.execute(new ConsultarDisponibilidadeCommand(id, data)));
    }
}
