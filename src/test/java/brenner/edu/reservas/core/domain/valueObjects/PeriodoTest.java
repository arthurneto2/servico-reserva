package brenner.edu.reservas.core.domain.valueObjects;

import brenner.edu.reservas.core.domain.exceptions.PeriodoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodoTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final FusoHorario LISBOA = FusoHorario.of("Europe/Lisbon");
    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);

    private static DataHora em(FusoHorario fuso, LocalDate dia, String hora) {
        LocalDateTime local = LocalDateTime.of(dia, LocalTime.parse(hora));
        return new DataHora(local.atZone(fuso.value()).toInstant(), fuso);
    }

    private static DataHora em(String hora) {
        return em(SAO_PAULO, DIA, hora);
    }

    private static Periodo periodo(String inicio, String fim) {
        return new Periodo(em(inicio), em(fim));
    }

    @Nested
    @DisplayName("fim estritamente depois de início")
    class Ordem {

        @Test
        void rejeitaInicioNulo() {
            assertThrows(PeriodoInvalidoException.class, () -> new Periodo(null, em("09:00")));
        }

        @Test
        void rejeitaFimNulo() {
            assertThrows(PeriodoInvalidoException.class, () -> new Periodo(em("09:00"), null));
        }

        @Test
        void rejeitaFimIgualAoInicio() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("09:00", "09:00"));
        }

        @Test
        void rejeitaFimAntesDoInicio() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("11:00", "09:00"));
        }
    }

    @Nested
    @DisplayName("mesmo fuso horário")
    class MesmoFuso {

        @Test
        void aceitaInicioEFimNoMesmoFuso() {
            assertDoesNotThrow(() -> periodo("09:00", "10:00"));
        }

        @Test
        void rejeitaFusosDiferentes() {
            DataHora inicio = em(SAO_PAULO, DIA, "09:00");
            DataHora fim = em(LISBOA, DIA, "14:00");

            assertThrows(PeriodoInvalidoException.class, () -> new Periodo(inicio, fim));
        }
    }

    @Nested
    @DisplayName("duração entre 30 minutos e 4 horas")
    class DuracaoPermitida {

        @Test
        void rejeitaDuracaoMenorQue30Minutos() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("09:00", "09:15"));
        }

        @Test
        void aceitaDuracaoDeExatamente30Minutos() {
            assertEquals(Duration.ofMinutes(30), periodo("09:00", "09:30").duracao());
        }

        @Test
        void aceitaDuracaoDeExatamente4Horas() {
            assertEquals(Duration.ofHours(4), periodo("08:00", "12:00").duracao());
        }

        @Test
        void rejeitaDuracaoMaiorQue4Horas() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("08:00", "12:15"));
        }
    }

    @Nested
    @DisplayName("início e fim no mesmo dia local")
    class MesmoDiaLocal {

        @Test
        void rejeitaPeriodoQueViraODia() {
            DataHora inicio = em(SAO_PAULO, DIA, "23:00");
            DataHora fim = em(SAO_PAULO, DIA.plusDays(1), "01:00");

            assertThrows(PeriodoInvalidoException.class, () -> new Periodo(inicio, fim));
        }
    }

    @Nested
    @DisplayName("início alinhado a múltiplos de 15 minutos")
    class AlinhamentoDoInicio {

        @Test
        void aceitaMultiplosDe15() {
            assertAll(
                    () -> assertDoesNotThrow(() -> periodo("09:00", "10:00")),
                    () -> assertDoesNotThrow(() -> periodo("09:15", "10:00")),
                    () -> assertDoesNotThrow(() -> periodo("09:30", "10:00")),
                    () -> assertDoesNotThrow(() -> periodo("09:45", "10:30")));
        }

        @Test
        void rejeitaMinutoNaoAlinhado() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("09:10", "10:00"));
        }

        @Test
        void rejeitaSegundosNoInicio() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("09:00:30", "10:00:30"));
        }

        @Test
        void naoExigeAlinhamentoDoFim() {
            assertDoesNotThrow(() -> periodo("09:00", "09:50"));
        }
    }

    @Nested
    @DisplayName("contido no horário de funcionamento 08:00–20:00 local")
    class HorarioDeFuncionamento {

        @Test
        void aceitaPeriodoQueComecaNaAbertura() {
            assertDoesNotThrow(() -> periodo("08:00", "09:00"));
        }

        @Test
        void aceitaPeriodoQueTerminaNoFechamento() {
            assertDoesNotThrow(() -> periodo("19:00", "20:00"));
        }

        @Test
        void rejeitaInicioAntesDaAbertura() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("07:45", "08:30"));
        }

        @Test
        void rejeitaFimDepoisDoFechamento() {
            assertThrows(PeriodoInvalidoException.class, () -> periodo("19:00", "20:15"));
        }

        @Test
        void avaliaOHorarioNoFusoLocalDoPeriodo() {
            // 09:00–10:00 em Lisboa é 05:00–06:00 em São Paulo: válido no fuso do próprio período.
            assertDoesNotThrow(() -> new Periodo(em(LISBOA, DIA, "09:00"), em(LISBOA, DIA, "10:00")));
        }
    }

    @Nested
    @DisplayName("duracao()")
    class Duracao {

        @Test
        void retornaADuracaoEntreInicioEFim() {
            assertEquals(Duration.ofMinutes(90), periodo("09:00", "10:30").duracao());
        }
    }

    @Nested
    @DisplayName("sobrepoe(Periodo)")
    class Sobreposicao {

        @Test
        void detectaSobreposicaoParcial() {
            Periodo manha = periodo("09:00", "11:00");
            Periodo meio = periodo("10:00", "12:00");

            assertAll(
                    () -> assertTrue(manha.sobrepoe(meio)),
                    () -> assertTrue(meio.sobrepoe(manha)));
        }

        @Test
        void detectaPeriodoContidoEmOutro() {
            Periodo maior = periodo("09:00", "13:00");
            Periodo menor = periodo("10:00", "11:00");

            assertAll(
                    () -> assertTrue(maior.sobrepoe(menor)),
                    () -> assertTrue(menor.sobrepoe(maior)));
        }

        @Test
        void naoSobrepoeQuandoApenasEncostam() {
            Periodo antes = periodo("09:00", "11:00");
            Periodo depois = periodo("11:00", "12:00");

            assertAll(
                    () -> assertFalse(antes.sobrepoe(depois)),
                    () -> assertFalse(depois.sobrepoe(antes)));
        }

        @Test
        void naoSobrepoeQuandoDisjuntos() {
            Periodo manha = periodo("09:00", "10:00");
            Periodo tarde = periodo("14:00", "15:00");

            assertAll(
                    () -> assertFalse(manha.sobrepoe(tarde)),
                    () -> assertFalse(tarde.sobrepoe(manha)));
        }

        @Test
        void sobrepoeASiMesmo() {
            Periodo periodo = periodo("09:00", "10:00");

            assertTrue(periodo.sobrepoe(periodo));
        }

        @Test
        void comparaInstantesMesmoComFusosDiferentes() {
            // Em março Lisboa está em UTC+0 e São Paulo em UTC-3: 12:30–13:30 em Lisboa
            // é 09:30–10:30 em São Paulo, ou seja, meia hora de sobreposição com 09:00–10:00.
            Periodo saoPaulo = periodo("09:00", "10:00");
            Periodo lisboa = new Periodo(em(LISBOA, DIA, "12:30"), em(LISBOA, DIA, "13:30"));

            assertAll(
                    () -> assertTrue(saoPaulo.sobrepoe(lisboa)),
                    () -> assertTrue(lisboa.sobrepoe(saoPaulo)));
        }

        @Test
        void rejeitaPeriodoNulo() {
            Periodo periodo = periodo("09:00", "10:00");

            assertThrows(PeriodoInvalidoException.class, () -> periodo.sobrepoe(null));
        }
    }
}
