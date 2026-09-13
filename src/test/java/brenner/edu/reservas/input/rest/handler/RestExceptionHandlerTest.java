package brenner.edu.reservas.input.rest.handler;

import brenner.edu.reservas.core.domain.exceptions.ConflitoDeHorarioException;
import brenner.edu.reservas.core.domain.exceptions.DataNoPassadoException;
import brenner.edu.reservas.core.domain.exceptions.ReservaExpiradaException;
import brenner.edu.reservas.core.domain.exceptions.SolicitanteNaoEOrganizadorException;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.usecase.CancelarReservaUseCase;
import brenner.edu.reservas.core.usecase.ConsultarDisponibilidadeUseCase;
import brenner.edu.reservas.core.usecase.CriarReservaUseCase;
import brenner.edu.reservas.core.usecase.RealizarCheckInUseCase;
import brenner.edu.reservas.core.usecase.exceptions.GetSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.ReservaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.SalaNotFoundException;
import brenner.edu.reservas.input.rest.controller.CancelarReservaController;
import brenner.edu.reservas.input.rest.controller.ConsultarDisponibilidadeController;
import brenner.edu.reservas.input.rest.controller.CriarReservaController;
import brenner.edu.reservas.input.rest.controller.RealizarCheckInController;
import brenner.edu.reservas.input.rest.mapper.DisponibilidadeResponseMapper;
import brenner.edu.reservas.input.rest.mapper.ReservaResponseMapper;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CriarReservaController.class, CancelarReservaController.class,
        RealizarCheckInController.class, ConsultarDisponibilidadeController.class})
@Import({ReservaResponseMapper.class, DisponibilidadeResponseMapper.class})
class RestExceptionHandlerTest {

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
    private CriarReservaUseCase criarReserva;

    @MockitoBean
    private CancelarReservaUseCase cancelarReserva;

    @MockitoBean
    private RealizarCheckInUseCase realizarCheckIn;

    @MockitoBean
    private ConsultarDisponibilidadeUseCase consultarDisponibilidade;

    private ListAppender<ILoggingEvent> logDoHandler;

    @BeforeEach
    void escutaOLog() {
        logDoHandler = new ListAppender<>();
        logDoHandler.start();
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RestExceptionHandler.class))
                .addAppender(logDoHandler);
    }

    @AfterEach
    void paraDeEscutar() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(RestExceptionHandler.class))
                .detachAppender(logDoHandler);
    }

    private org.springframework.test.web.servlet.ResultActions criar() throws Exception {
        return mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content(CORPO));
    }

    @Nested
    @DisplayName("formato da resposta de erro")
    class Formato {

        @Test
        void todoErroSaiComoProblemJson() throws Exception {
            when(criarReserva.execute(any())).thenThrow(new SalaNotFoundException(new SalaId(SALA)));

            criar().andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        void carregaStatusTitleDetailETimestamp() throws Exception {
            when(criarReserva.execute(any())).thenThrow(new SalaNotFoundException(new SalaId(SALA)));

            criar().andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.detail").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void oTimestampEUmInstanteLegivel() throws Exception {
            when(criarReserva.execute(any())).thenThrow(new SalaNotFoundException(new SalaId(SALA)));

            String timestamp = criar().andReturn().getResponse().getContentAsString()
                    .replaceAll(".*\"timestamp\"\\s*:\\s*\"([^\"]+)\".*", "$1");

            assertDoesNotThrow(() -> Instant.parse(timestamp));
        }

        @Test
        void erroDeFormaTambemVemCarimbado() throws Exception {
            // Os 400 do próprio Spring MVC passam pelo mesmo tratamento, para o cliente não
            // receber dois formatos de erro diferentes da mesma API.
            mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content("{ não é json }"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("404: recurso não encontrado")
    class NaoEncontrado {

        @Test
        void salaInexistenteNaCriacao() throws Exception {
            when(criarReserva.execute(any())).thenThrow(new SalaNotFoundException(new SalaId(SALA)));

            criar().andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(SALA.toString())));
        }

        @Test
        void reservaInexistenteNoCancelamento() throws Exception {
            doThrow(new ReservaNotFoundException(new ReservaId(RESERVA)))
                    .when(cancelarReserva).execute(any());

            mockMvc.perform(delete("/reservas/{id}", RESERVA).header("X-Solicitante", "ana@empresa.com"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("422: regra de negócio violada")
    class RegraDeNegocio {

        @Test
        void conflitoDeHorario() throws Exception {
            when(criarReserva.execute(any()))
                    .thenThrow(new ConflitoDeHorarioException("Período conflita com a reserva X"));

            criar().andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Período conflita com a reserva X"));
        }

        @Test
        void solicitanteNaoEOrganizador() throws Exception {
            doThrow(new SolicitanteNaoEOrganizadorException("Apenas o organizador pode cancelar"))
                    .when(cancelarReserva).execute(any());

            mockMvc.perform(delete("/reservas/{id}", RESERVA).header("X-Solicitante", "bruno@empresa.com"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void reservaExpiradaNoCheckIn() throws Exception {
            when(realizarCheckIn.execute(any()))
                    .thenThrow(new ReservaExpiradaException("Check-in encerrou às 14:15"));

            mockMvc.perform(post("/reservas/{id}/check-in", RESERVA))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Check-in encerrou às 14:15"));
        }

        @Test
        void dataNoPassadoNaConsulta() throws Exception {
            when(consultarDisponibilidade.execute(any()))
                    .thenThrow(new DataNoPassadoException("Data 2020-01-01 já passou"));

            mockMvc.perform(get("/salas/{id}/disponibilidade", SALA).param("data", "2020-01-01"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("400: forma inválida")
    class Forma {

        @Test
        void campoObrigatorioAusenteNoCorpo() throws Exception {
            String semOrganizador = """
                    {"salaId": "11111111-1111-1111-1111-111111111111", "inicio": "2026-09-15T14:00",
                     "fim": "2026-09-15T15:30", "quantidadeParticipantes": 6}
                    """;

            mockMvc.perform(post("/reservas").contentType(APPLICATION_JSON).content(semOrganizador))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("organizador")));
        }

        @Test
        void headerObrigatorioAusente() throws Exception {
            mockMvc.perform(delete("/reservas/{id}", RESERVA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        void queryParamAusente() throws Exception {
            mockMvc.perform(get("/salas/{id}/disponibilidade", SALA))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void commandQueRecusaAForma() throws Exception {
            // O Command valida forma lançando IllegalArgumentException/NullPointerException;
            // o handler precisa tratá-las como 400, e não deixá-las virar 500.
            when(criarReserva.execute(any()))
                    .thenThrow(new IllegalArgumentException("quantidadeParticipantes deve ser > 0"));

            criar().andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("quantidadeParticipantes deve ser > 0"));
        }
    }

    @Nested
    @DisplayName("503: dependência indisponível")
    class Indisponivel {

        private static final RuntimeException CAUSA = new IllegalStateException("banco fora do ar");

        @Test
        void portIndisponivelViraServiceUnavailable() throws Exception {
            when(criarReserva.execute(any())).thenThrow(new GetSalaUnavailableException(CAUSA));

            criar().andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503));
        }

        @Test
        void aCausaTecnicaVaiParaOLogENaoParaOCliente() throws Exception {
            // O adapter engoliu a exceção dentro de Result.failure: o log do handler é o único
            // lugar onde o operador descobre o que houve. E o cliente não precisa saber que o
            // problema foi no banco.
            when(criarReserva.execute(any())).thenThrow(new GetSalaUnavailableException(CAUSA));

            String corpo = criar().andReturn().getResponse().getContentAsString();

            assertAll(
                    () -> assertFalse(corpo.contains("banco fora do ar"),
                            "a causa técnica não deve vazar para o cliente"),
                    () -> assertTrue(logDoHandler.list.stream()
                                    .anyMatch(evento -> evento.getLevel() == Level.ERROR
                                            && evento.getThrowableProxy() != null
                                            && evento.getThrowableProxy().getMessage().contains("banco fora do ar")),
                            "a causa técnica precisa aparecer no log, em nível ERROR"));
        }

        @Test
        void bancoInacessivelAoAbrirATransacaoTambemE503() throws Exception {
            // Esta falha não passa por adapter nenhum: o @Transactional do UseCase pede uma
            // conexão antes de a primeira linha do UseCase rodar, então não há try/catch que a
            // transforme em Result.failure. Sem tratá-la aqui, banco fora do ar vira 500.
            when(criarReserva.execute(any())).thenThrow(
                    new org.springframework.transaction.CannotCreateTransactionException(
                            "Could not open JPA EntityManager", CAUSA));

            criar().andExpect(status().isServiceUnavailable());
        }

        @Test
        void erroDeAcessoAoBancoDentroDaTransacaoTambemE503() throws Exception {
            when(criarReserva.execute(any())).thenThrow(
                    new org.springframework.dao.DataAccessResourceFailureException("conexão perdida", CAUSA));

            criar().andExpect(status().isServiceUnavailable());
        }

        @Test
        void aFalhaDeInfraestruturaTambemVaiParaOLog() throws Exception {
            when(criarReserva.execute(any())).thenThrow(
                    new org.springframework.transaction.CannotCreateTransactionException(
                            "Could not open JPA EntityManager", CAUSA));

            criar();

            assertTrue(logDoHandler.list.stream()
                            .anyMatch(evento -> evento.getLevel() == Level.ERROR),
                    "a falha de infraestrutura precisa aparecer no log");
        }

        @Test
        void oDetailExplicaOQueFalhouSemDetalheTecnico() throws Exception {
            when(criarReserva.execute(any())).thenThrow(new GetSalaUnavailableException(CAUSA));

            criar().andExpect(jsonPath("$.detail")
                    .value(org.hamcrest.Matchers.containsString("busca da sala")));
        }
    }

    @Nested
    @DisplayName("o que o handler NÃO esconde")
    class NaoEsconde {

        @Test
        void erroInesperadoNaoEAdotadoPeloHandler() throws Exception {
            // Nada de capturar Exception e devolver 503: um bug do código não é indisponibilidade,
            // e transformá-lo em 503 esconderia defeito atrás de "tente de novo". Como nenhum
            // @ExceptionHandler o adota, a exceção escapa — no MockMvc ela sobe até aqui, e no
            // servidor cai no tratamento padrão do Spring, que é 500.
            when(criarReserva.execute(any())).thenThrow(new RuntimeException("bug"));

            Exception escapou = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                    RestExceptionHandlerTest.this::criar);

            assertTrue(escapou.getMessage().contains("bug")
                            || (escapou.getCause() != null && escapou.getCause().getMessage().contains("bug")),
                    "a exceção inesperada deve escapar do handler, não virar 503");
        }
    }
}
