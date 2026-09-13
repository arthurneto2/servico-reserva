package brenner.edu.reservas.input.rest.controller;

import brenner.edu.reservas.core.usecase.CancelarReservaUseCase;
import brenner.edu.reservas.core.usecase.commands.CancelarReservaCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CancelarReservaController.class)
class CancelarReservaControllerTest {

    private static final UUID RESERVA = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CancelarReservaUseCase useCase;

    @Test
    void devolve204SemCorpo() throws Exception {
        mockMvc.perform(delete("/reservas/{id}", RESERVA).header("X-Solicitante", "ana@empresa.com"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void montaOCommandComOIdDaUrlEOSolicitanteDoHeader() throws Exception {
        mockMvc.perform(delete("/reservas/{id}", RESERVA).header("X-Solicitante", "ana@empresa.com"));

        ArgumentCaptor<CancelarReservaCommand> command = ArgumentCaptor.forClass(CancelarReservaCommand.class);
        verify(useCase).execute(command.capture());
        assertAll(
                () -> assertEquals(RESERVA, command.getValue().reservaId()),
                () -> assertEquals("ana@empresa.com", command.getValue().solicitante()));
    }

    @Test
    void semOHeaderDeSolicitanteNaoChegaAoUseCase() throws Exception {
        mockMvc.perform(delete("/reservas/{id}", RESERVA))
                .andExpect(status().isBadRequest());

        verify(useCase, never()).execute(any());
    }

    @Test
    void idQueNaoEUuidNaoChegaAoUseCase() throws Exception {
        mockMvc.perform(delete("/reservas/{id}", "nao-e-uuid").header("X-Solicitante", "ana@empresa.com"))
                .andExpect(status().isBadRequest());

        verify(useCase, never()).execute(any());
    }
}
