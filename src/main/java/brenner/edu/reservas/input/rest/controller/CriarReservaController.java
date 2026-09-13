package brenner.edu.reservas.input.rest.controller;

import brenner.edu.reservas.core.usecase.CriarReservaUseCase;
import brenner.edu.reservas.core.usecase.commands.CriarReservaCommand;
import brenner.edu.reservas.input.rest.dto.CriarReservaRequest;
import brenner.edu.reservas.input.rest.dto.ReservaResponse;
import brenner.edu.reservas.input.rest.mapper.ReservaResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * {@code POST /reservas}.
 *
 * <p>Um endpoint, um UseCase. O controller faz três coisas e nada mais: traduz o DTO em Command,
 * chama {@code execute} e traduz a resposta com o mapper. Ele não conhece {@code core.domain} nem
 * os ports — regra verificada pelo {@code ArchitectureTest} —, então também não tem como conter
 * regra de negócio nem decidir status a partir dela: erro vira exceção e quem a traduz é o
 * {@code @ControllerAdvice}.
 */
@RestController
@RequestMapping("/reservas")
@Tag(name = "Reservas", description = "Criação, cancelamento e check-in de reservas de salas")
public class CriarReservaController {

    private final CriarReservaUseCase useCase;
    private final ReservaResponseMapper mapper;

    public CriarReservaController(CriarReservaUseCase useCase, ReservaResponseMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @Operation(summary = "Cria uma reserva",
            description = """
                    Os horários são locais ao fuso da sala, no formato yyyy-MM-dd'T'HH:mm.
                    A reserva nasce PENDENTE e precisa de check-in entre 10 minutos antes e
                    15 minutos depois do início.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva criada; o header Location aponta para ela"),
            @ApiResponse(responseCode = "400", description = "Corpo malformado ou campo obrigatório ausente",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "404", description = "Sala não encontrada",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "422",
                    description = "Sala inativa, capacidade excedida, antecedência mínima, conflito de "
                            + "horário, período inválido ou e-mail inválido",
                    content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "503", description = "Dependência externa indisponível",
                    content = @io.swagger.v3.oas.annotations.media.Content)})
    @PostMapping
    public ResponseEntity<ReservaResponse> criar(@Valid @RequestBody CriarReservaRequest request) {
        var reserva = useCase.execute(new CriarReservaCommand(request.salaId(),
                request.organizador(),
                request.inicio(),
                request.fim(),
                request.quantidadeParticipantes()));

        var response = mapper.toResponse(reserva);
        return ResponseEntity.created(URI.create("/reservas/" + response.id())).body(response);
    }
}
