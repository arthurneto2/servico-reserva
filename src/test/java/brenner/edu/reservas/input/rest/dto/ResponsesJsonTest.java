package brenner.edu.reservas.input.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O formato das respostas é contrato: datas locais ao fuso da sala em {@code yyyy-MM-dd'T'HH:mm},
 * sem segundos e sem offset, e o fuso sempre presente em um campo próprio.
 */
class ResponsesJsonTest {

    private static final UUID RESERVA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SALA = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    private JsonNode serializa(Object dto) throws Exception {
        return json.readTree(json.writeValueAsString(dto));
    }

    @Nested
    @DisplayName("ReservaResponse")
    class DaReserva {

        private final ReservaResponse response = new ReservaResponse(RESERVA, SALA, "ana@empresa.com",
                LocalDateTime.of(2026, 9, 15, 14, 0),
                LocalDateTime.of(2026, 9, 15, 15, 30),
                "America/Sao_Paulo", 6, "PENDENTE",
                LocalDateTime.of(2026, 9, 12, 9, 3));

        @Test
        void temTodosOsCamposDoContrato() throws Exception {
            JsonNode corpo = serializa(response);

            assertAll(
                    () -> assertEquals(RESERVA.toString(), corpo.get("id").asText()),
                    () -> assertEquals(SALA.toString(), corpo.get("salaId").asText()),
                    () -> assertEquals("ana@empresa.com", corpo.get("organizador").asText()),
                    () -> assertEquals("America/Sao_Paulo", corpo.get("fuso").asText()),
                    () -> assertEquals(6, corpo.get("quantidadeParticipantes").asInt()),
                    () -> assertEquals("PENDENTE", corpo.get("status").asText()));
        }

        @Test
        void escreveDatasSemSegundosESemOffset() throws Exception {
            JsonNode corpo = serializa(response);

            assertAll(
                    () -> assertEquals("2026-09-15T14:00", corpo.get("inicio").asText()),
                    () -> assertEquals("2026-09-15T15:30", corpo.get("fim").asText()),
                    () -> assertEquals("2026-09-12T09:03", corpo.get("criadaEm").asText()));
        }
    }

    @Nested
    @DisplayName("DisponibilidadeResponse")
    class DaDisponibilidade {

        private final DisponibilidadeResponse response = new DisponibilidadeResponse(
                SALA, "Sala Pirapora", "America/Sao_Paulo", LocalDate.of(2026, 9, 15),
                List.of(new JanelaLivreResponse(LocalDateTime.of(2026, 9, 15, 8, 0),
                                LocalDateTime.of(2026, 9, 15, 14, 0)),
                        new JanelaLivreResponse(LocalDateTime.of(2026, 9, 15, 15, 30),
                                LocalDateTime.of(2026, 9, 15, 20, 0))),
                List.of(new ReservaResumoResponse(RESERVA, LocalDateTime.of(2026, 9, 15, 14, 0),
                        LocalDateTime.of(2026, 9, 15, 15, 30), "PENDENTE")));

        @Test
        void temOsDadosDaSalaEODiaConsultado() throws Exception {
            JsonNode corpo = serializa(response);

            assertAll(
                    () -> assertEquals(SALA.toString(), corpo.get("salaId").asText()),
                    () -> assertEquals("Sala Pirapora", corpo.get("nome").asText()),
                    () -> assertEquals("America/Sao_Paulo", corpo.get("fuso").asText()),
                    () -> assertEquals("2026-09-15", corpo.get("data").asText()));
        }

        @Test
        void listaAsJanelasLivresNaOrdemRecebida() throws Exception {
            JsonNode janelas = serializa(response).get("horariosLivres");

            assertAll(
                    () -> assertEquals(2, janelas.size()),
                    () -> assertEquals("2026-09-15T08:00", janelas.get(0).get("inicio").asText()),
                    () -> assertEquals("2026-09-15T14:00", janelas.get(0).get("fim").asText()),
                    () -> assertEquals("2026-09-15T15:30", janelas.get(1).get("inicio").asText()),
                    () -> assertEquals("2026-09-15T20:00", janelas.get(1).get("fim").asText()));
        }

        @Test
        void resumeAsReservasSemRepetirSalaEFuso() throws Exception {
            JsonNode reservas = serializa(response).get("reservas");

            assertAll(
                    () -> assertEquals(1, reservas.size()),
                    () -> assertEquals(RESERVA.toString(), reservas.get(0).get("id").asText()),
                    () -> assertEquals("2026-09-15T14:00", reservas.get(0).get("inicio").asText()),
                    () -> assertEquals("PENDENTE", reservas.get(0).get("status").asText()),
                    () -> assertEquals(4, reservas.get(0).size()));
        }
    }
}
