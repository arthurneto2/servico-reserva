package brenner.edu.reservas.output.time;

import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.shared.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetDataHoraAtualAdapterTest {

    private static final Instant AGORA = Instant.parse("2026-09-15T17:00:00Z");
    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final FusoHorario LONDRES = FusoHorario.of("Europe/London");

    private final GetDataHoraAtualAdapter adapter =
            new GetDataHoraAtualAdapter(Clock.fixed(AGORA, ZoneOffset.UTC));

    private DataHora agoraEm(FusoHorario fuso) {
        return adapter.get(fuso).getAvailableValueOrElseThrow(RuntimeException::new).orElseThrow();
    }

    @Test
    void devolveOInstanteAtual() {
        assertEquals(AGORA, agoraEm(SAO_PAULO).instante());
    }

    @Test
    void expressaOInstanteNoFusoPedido() {
        assertAll(
                () -> assertEquals(SAO_PAULO, agoraEm(SAO_PAULO).fusoHorario()),
                () -> assertEquals(LocalTime.of(14, 0), agoraEm(SAO_PAULO).hora()));
    }

    @Test
    void oMesmoInstanteEOutraHoraLocalEmOutroFuso() {
        // É por isso que o relógio é um port que recebe o fuso, e não um Instant.now() solto.
        assertAll(
                () -> assertEquals(LocalTime.of(14, 0), agoraEm(SAO_PAULO).hora()),
                () -> assertEquals(LocalTime.of(18, 0), agoraEm(LONDRES).hora()),
                () -> assertEquals(agoraEm(SAO_PAULO).instante(), agoraEm(LONDRES).instante()));
    }

    @Test
    void fusoNuloEFalhaENaoExcecao() {
        Result<DataHora> resultado = adapter.get(null);

        assertTrue(resultado.isFailure());
    }

    @Test
    void oRelogioDoSistemaEOPadrao() {
        GetDataHoraAtualAdapter doSistema = new GetDataHoraAtualAdapter();

        DataHora agora = doSistema.get(SAO_PAULO)
                .getAvailableValueOrElseThrow(RuntimeException::new).orElseThrow();

        assertTrue(Math.abs(agora.instante().toEpochMilli() - System.currentTimeMillis()) < 60_000);
    }
}
