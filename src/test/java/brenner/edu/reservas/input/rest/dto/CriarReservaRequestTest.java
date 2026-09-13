package brenner.edu.reservas.input.rest.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriarReservaRequestTest {

    private static final UUID SALA = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static ValidatorFactory factory;
    private static Validator validator;

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeAll
    static void abreOValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fechaOValidador() {
        factory.close();
    }

    private Set<String> camposInvalidos(CriarReservaRequest request) {
        return validator.validate(request).stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static CriarReservaRequest completo() {
        return new CriarReservaRequest(SALA, "ana@empresa.com",
                LocalDateTime.of(2026, 9, 15, 14, 0),
                LocalDateTime.of(2026, 9, 15, 15, 30), 6);
    }

    @Nested
    @DisplayName("desserialização")
    class Desserializacao {

        @Test
        void leOCorpoDoContrato() throws Exception {
            String corpo = """
                    {
                      "salaId": "11111111-1111-1111-1111-111111111111",
                      "organizador": "ana@empresa.com",
                      "inicio": "2026-09-15T14:00",
                      "fim": "2026-09-15T15:30",
                      "quantidadeParticipantes": 6
                    }
                    """;

            CriarReservaRequest request = json.readValue(corpo, CriarReservaRequest.class);

            assertAll(
                    () -> assertEquals(SALA, request.salaId()),
                    () -> assertEquals("ana@empresa.com", request.organizador()),
                    () -> assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), request.inicio()),
                    () -> assertEquals(LocalDateTime.of(2026, 9, 15, 15, 30), request.fim()),
                    () -> assertEquals(6, request.quantidadeParticipantes()));
        }

        @Test
        void osHorariosNaoCarregamOffset() throws Exception {
            // "2026-09-15T14:00" é hora local da sala: quem sabe o fuso é o UseCase, com a Sala
            // em mãos. Um offset no corpo daria ao cliente um poder que ele não tem.
            String corpo = """
                    {"salaId": "11111111-1111-1111-1111-111111111111", "organizador": "ana@empresa.com",
                     "inicio": "2026-09-15T14:00-03:00", "fim": "2026-09-15T15:30", "quantidadeParticipantes": 6}
                    """;

            assertTrue(org.junit.jupiter.api.Assertions
                    .assertThrows(Exception.class, () -> json.readValue(corpo, CriarReservaRequest.class))
                    .getMessage().contains("inicio"));
        }
    }

    @Nested
    @DisplayName("Bean Validation: só forma, e o que ela rejeita vira 400")
    class Forma {

        @Test
        void aceitaOPedidoCompleto() {
            assertTrue(camposInvalidos(completo()).isEmpty());
        }

        @Test
        void rejeitaCamposAusentes() {
            CriarReservaRequest request = new CriarReservaRequest(null, null, null, null, 6);

            assertEquals(Set.of("salaId", "organizador", "inicio", "fim"), camposInvalidos(request));
        }

        @Test
        void rejeitaOrganizadorEmBranco() {
            CriarReservaRequest request = new CriarReservaRequest(SALA, "   ",
                    LocalDateTime.of(2026, 9, 15, 14, 0), LocalDateTime.of(2026, 9, 15, 15, 30), 6);

            assertEquals(Set.of("organizador"), camposInvalidos(request));
        }

        @Test
        void rejeitaQuantidadeNaoPositiva() {
            CriarReservaRequest request = new CriarReservaRequest(SALA, "ana@empresa.com",
                    LocalDateTime.of(2026, 9, 15, 14, 0), LocalDateTime.of(2026, 9, 15, 15, 30), 0);

            assertEquals(Set.of("quantidadeParticipantes"), camposInvalidos(request));
        }
    }

    @Nested
    @DisplayName("o que o DTO deliberadamente NÃO valida")
    class NaoValida {

        @Test
        void emailMalformadoPassaPeloDtoEMorreNoValueObject() {
            // Formato de e-mail é regra de negócio do VO Email → 422, não 400. Um @Email aqui
            // mudaria o status da resposta e tiraria a regra do domínio.
            CriarReservaRequest request = new CriarReservaRequest(SALA, "ana(at)empresa",
                    LocalDateTime.of(2026, 9, 15, 14, 0), LocalDateTime.of(2026, 9, 15, 15, 30), 6);

            assertTrue(camposInvalidos(request).isEmpty());
        }

        @Test
        void fimAntesDoInicioPassaPeloDtoEMorreNoPeriodo() {
            // Ordem, duração, alinhamento e horário de funcionamento são invariantes de Periodo → 422.
            CriarReservaRequest request = new CriarReservaRequest(SALA, "ana@empresa.com",
                    LocalDateTime.of(2026, 9, 15, 15, 30), LocalDateTime.of(2026, 9, 15, 14, 0), 6);

            assertTrue(camposInvalidos(request).isEmpty());
        }

        @Test
        void horarioForaDoFuncionamentoPassaPeloDtoEMorreNoPeriodo() {
            CriarReservaRequest request = new CriarReservaRequest(SALA, "ana@empresa.com",
                    LocalDateTime.of(2026, 9, 15, 5, 0), LocalDateTime.of(2026, 9, 15, 6, 0), 6);

            assertTrue(camposInvalidos(request).isEmpty());
        }
    }
}
