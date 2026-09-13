package brenner.edu.reservas.input.rest.controller;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.Capacidade;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.JanelaLivre;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.usecase.ConsultarDisponibilidadeUseCase;
import brenner.edu.reservas.core.usecase.commands.ConsultarDisponibilidadeCommand;
import brenner.edu.reservas.core.usecase.results.DisponibilidadeDaSala;
import brenner.edu.reservas.input.rest.mapper.DisponibilidadeResponseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultarDisponibilidadeController.class)
@Import(DisponibilidadeResponseMapper.class)
class ConsultarDisponibilidadeControllerTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID SALA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDate DIA = LocalDate.of(2026, 9, 15);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultarDisponibilidadeUseCase useCase;

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static DisponibilidadeDaSala disponibilidade() {
        Sala sala = new Sala(new SalaId(SALA), "Sala Pirapora", new Capacidade(8), SAO_PAULO, true);
        Reserva reserva = Reserva.reconstituir(new ReservaId(RESERVA), new SalaId(SALA),
                new Email("ana@empresa.com"),
                new Periodo(em("2026-09-15T14:00"), em("2026-09-15T15:30")),
                new QuantidadeParticipantes(6), StatusReserva.PENDENTE, em("2026-09-14T10:00"));

        return new DisponibilidadeDaSala(sala, DIA,
                List.of(new JanelaLivre(em("2026-09-15T08:00"), em("2026-09-15T14:00")),
                        new JanelaLivre(em("2026-09-15T15:30"), em("2026-09-15T20:00"))),
                List.of(reserva));
    }

    @Test
    void devolve200ComAAgendaDoDia() throws Exception {
        when(useCase.execute(any())).thenReturn(disponibilidade());

        mockMvc.perform(get("/salas/{id}/disponibilidade", SALA).param("data", "2026-09-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salaId").value(SALA.toString()))
                .andExpect(jsonPath("$.nome").value("Sala Pirapora"))
                .andExpect(jsonPath("$.fuso").value("America/Sao_Paulo"))
                .andExpect(jsonPath("$.data").value("2026-09-15"));
    }

    @Test
    void listaJanelasLivresEReservas() throws Exception {
        when(useCase.execute(any())).thenReturn(disponibilidade());

        mockMvc.perform(get("/salas/{id}/disponibilidade", SALA).param("data", "2026-09-15"))
                .andExpect(jsonPath("$.horariosLivres.length()").value(2))
                .andExpect(jsonPath("$.horariosLivres[0].inicio").value("2026-09-15T08:00"))
                .andExpect(jsonPath("$.horariosLivres[0].fim").value("2026-09-15T14:00"))
                .andExpect(jsonPath("$.reservas.length()").value(1))
                .andExpect(jsonPath("$.reservas[0].id").value(RESERVA.toString()))
                .andExpect(jsonPath("$.reservas[0].status").value("PENDENTE"));
    }

    @Test
    void montaOCommandComOIdDaUrlEADataDaQuery() throws Exception {
        when(useCase.execute(any())).thenReturn(disponibilidade());

        mockMvc.perform(get("/salas/{id}/disponibilidade", SALA).param("data", "2026-09-15"));

        ArgumentCaptor<ConsultarDisponibilidadeCommand> command =
                ArgumentCaptor.forClass(ConsultarDisponibilidadeCommand.class);
        verify(useCase).execute(command.capture());
        assertAll(
                () -> assertEquals(SALA, command.getValue().salaId()),
                () -> assertEquals(DIA, command.getValue().data()));
    }

    @Test
    void semADataNaoChegaAoUseCase() throws Exception {
        mockMvc.perform(get("/salas/{id}/disponibilidade", SALA))
                .andExpect(status().isBadRequest());

        verify(useCase, never()).execute(any());
    }

    @Test
    void dataEmFormatoInvalidoNaoChegaAoUseCase() throws Exception {
        mockMvc.perform(get("/salas/{id}/disponibilidade", SALA).param("data", "15/09/2026"))
                .andExpect(status().isBadRequest());

        verify(useCase, never()).execute(any());
    }
}
