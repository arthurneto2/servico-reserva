package brenner.edu.reservas.input.rest.controller;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.usecase.RealizarCheckInUseCase;
import brenner.edu.reservas.core.usecase.commands.RealizarCheckInCommand;
import brenner.edu.reservas.input.rest.mapper.ReservaResponseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RealizarCheckInController.class)
@Import(ReservaResponseMapper.class)
class RealizarCheckInControllerTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID SALA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVA = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RealizarCheckInUseCase useCase;

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static Reserva reservaConfirmada() {
        return Reserva.reconstituir(new ReservaId(RESERVA), new SalaId(SALA), new Email("ana@empresa.com"),
                new Periodo(em("2026-09-15T14:00"), em("2026-09-15T15:30")),
                new QuantidadeParticipantes(6), StatusReserva.CONFIRMADA, em("2026-09-12T09:03"));
    }

    @Test
    void devolve200ComAReservaConfirmada() throws Exception {
        when(useCase.execute(any())).thenReturn(reservaConfirmada());

        mockMvc.perform(post("/reservas/{id}/check-in", RESERVA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESERVA.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"));
    }

    @Test
    void naoDevolveLocationPorqueNadaFoiCriado() throws Exception {
        when(useCase.execute(any())).thenReturn(reservaConfirmada());

        mockMvc.perform(post("/reservas/{id}/check-in", RESERVA))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void montaOCommandComOIdDaUrl() throws Exception {
        when(useCase.execute(any())).thenReturn(reservaConfirmada());

        mockMvc.perform(post("/reservas/{id}/check-in", RESERVA));

        ArgumentCaptor<RealizarCheckInCommand> command = ArgumentCaptor.forClass(RealizarCheckInCommand.class);
        verify(useCase).execute(command.capture());
        assertEquals(RESERVA, command.getValue().reservaId());
    }

    @Test
    void naoAceitaCorpoNoPedido() throws Exception {
        // O "agora" do check-in vem do port de tempo, nunca do cliente: não há o que enviar.
        when(useCase.execute(any())).thenReturn(reservaConfirmada());

        mockMvc.perform(post("/reservas/{id}/check-in", RESERVA))
                .andExpect(status().isOk());
    }

    @Test
    void idQueNaoEUuidNaoChegaAoUseCase() throws Exception {
        mockMvc.perform(post("/reservas/{id}/check-in", "nao-e-uuid"))
                .andExpect(status().isBadRequest());

        verify(useCase, never()).execute(any());
    }
}
