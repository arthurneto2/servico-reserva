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
import brenner.edu.reservas.core.usecase.CriarReservaUseCase;
import brenner.edu.reservas.core.usecase.commands.CriarReservaCommand;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CriarReservaController.class)
@Import(ReservaResponseMapper.class)
class CriarReservaControllerTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID SALA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVA = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final String CORPO = """
            {
              "salaId": "11111111-1111-1111-1111-111111111111",
              "organizador": "ana@empresa.com",
              "inicio": "2026-09-15T14:00",
              "fim": "2026-09-15T15:30",
              "quantidadeParticipantes": 6
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CriarReservaUseCase useCase;

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static Reserva reservaCriada() {
        return Reserva.reconstituir(new ReservaId(RESERVA), new SalaId(SALA), new Email("ana@empresa.com"),
                new Periodo(em("2026-09-15T14:00"), em("2026-09-15T15:30")),
                new QuantidadeParticipantes(6), StatusReserva.PENDENTE, em("2026-09-12T09:03"));
    }

    @Test
    void devolve201ComALocationDaReservaCriada() throws Exception {
        when(useCase.execute(any())).thenReturn(reservaCriada());

        mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content(CORPO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/reservas/" + RESERVA));
    }

    @Test
    void devolveAReservaNoCorpo() throws Exception {
        when(useCase.execute(any())).thenReturn(reservaCriada());

        mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content(CORPO))
                .andExpect(jsonPath("$.id").value(RESERVA.toString()))
                .andExpect(jsonPath("$.salaId").value(SALA.toString()))
                .andExpect(jsonPath("$.organizador").value("ana@empresa.com"))
                .andExpect(jsonPath("$.inicio").value("2026-09-15T14:00"))
                .andExpect(jsonPath("$.fim").value("2026-09-15T15:30"))
                .andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.quantidadeParticipantes").value(6))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.criadaEm").value("2026-09-12T09:03"));
    }

    @Test
    void montaOCommandComOsDadosDoCorpo() throws Exception {
        when(useCase.execute(any())).thenReturn(reservaCriada());

        mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content(CORPO));

        ArgumentCaptor<CriarReservaCommand> command = ArgumentCaptor.forClass(CriarReservaCommand.class);
        verify(useCase).execute(command.capture());
        assertAll(
                () -> assertEquals(SALA, command.getValue().salaId()),
                () -> assertEquals("ana@empresa.com", command.getValue().organizador()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), command.getValue().inicio()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 15, 30), command.getValue().fim()),
                () -> assertEquals(6, command.getValue().quantidadeParticipantes()));
    }

    @Test
    void corpoSemCampoObrigatorioNaoChegaAoUseCase() throws Exception {
        String semOrganizador = """
                {"salaId": "11111111-1111-1111-1111-111111111111", "inicio": "2026-09-15T14:00",
                 "fim": "2026-09-15T15:30", "quantidadeParticipantes": 6}
                """;

        mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content(semOrganizador))
                .andExpect(status().isBadRequest());

        verify(useCase, org.mockito.Mockito.never()).execute(any());
    }

    @Test
    void corpoMalformadoNaoChegaAoUseCase() throws Exception {
        mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content("{ isto não é json }"))
                .andExpect(status().isBadRequest());

        verify(useCase, org.mockito.Mockito.never()).execute(any());
    }
}
