package brenner.edu.reservas.input.rest.mapper;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.input.rest.dto.ReservaResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservaResponseMapperTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final FusoHorario NOVA_YORK = FusoHorario.of("America/New_York");
    private static final UUID RESERVA_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SALA_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ReservaResponseMapper mapper = new ReservaResponseMapper();

    private static DataHora em(String local, FusoHorario fuso) {
        return new DataHora(LocalDateTime.parse(local).atZone(fuso.value()).toInstant(), fuso);
    }

    private static Reserva reserva(FusoHorario fuso, StatusReserva status) {
        return Reserva.reconstituir(new ReservaId(RESERVA_UUID), new SalaId(SALA_UUID),
                new Email("ana@empresa.com"),
                new Periodo(em("2026-09-15T14:00", fuso), em("2026-09-15T15:30", fuso)),
                new QuantidadeParticipantes(6), status, em("2026-09-12T09:03", fuso));
    }

    @Test
    void mapeiaTodosOsCampos() {
        ReservaResponse response = mapper.toResponse(reserva(SAO_PAULO, StatusReserva.PENDENTE));

        assertAll(
                () -> assertEquals(RESERVA_UUID, response.id()),
                () -> assertEquals(SALA_UUID, response.salaId()),
                () -> assertEquals("ana@empresa.com", response.organizador()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), response.inicio()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 15, 30), response.fim()),
                () -> assertEquals("America/Sao_Paulo", response.fuso()),
                () -> assertEquals(6, response.quantidadeParticipantes()),
                () -> assertEquals("PENDENTE", response.status()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 12, 9, 3), response.criadaEm()));
    }

    @Test
    void devolveHoraLocalDaSalaENaoUtc() {
        // A reserva em Nova York começa às 14:00 de lá — 18:00Z. A resposta mostra 14:00, e o
        // campo fuso é o que permite ao cliente saber de que 14:00 se trata.
        Reserva emNovaYork = reserva(NOVA_YORK, StatusReserva.PENDENTE);

        ReservaResponse response = mapper.toResponse(emNovaYork);

        assertAll(
                () -> assertEquals(Instant.parse("2026-09-15T18:00:00Z"),
                        emNovaYork.periodo().inicio().instante()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), response.inicio()),
                () -> assertEquals("America/New_York", response.fuso()));
    }

    @Test
    void oMesmoHorarioLocalEmFusosDiferentesTemARespostaIgual() {
        // Este é o cenário de demonstração do README: mesmo JSON de entrada, instantes
        // diferentes no banco — e, na volta, mesmo horário local em respostas de fusos distintos.
        ReservaResponse pirapora = mapper.toResponse(reserva(SAO_PAULO, StatusReserva.PENDENTE));
        ReservaResponse novaYork = mapper.toResponse(reserva(NOVA_YORK, StatusReserva.PENDENTE));

        assertAll(
                () -> assertEquals(pirapora.inicio(), novaYork.inicio()),
                () -> org.junit.jupiter.api.Assertions.assertNotEquals(pirapora.fuso(), novaYork.fuso()));
    }

    @Test
    void mapeiaCadaStatusComoTexto() {
        assertAll(
                () -> assertEquals("CONFIRMADA",
                        mapper.toResponse(reserva(SAO_PAULO, StatusReserva.CONFIRMADA)).status()),
                () -> assertEquals("CANCELADA",
                        mapper.toResponse(reserva(SAO_PAULO, StatusReserva.CANCELADA)).status()),
                () -> assertEquals("EXPIRADA",
                        mapper.toResponse(reserva(SAO_PAULO, StatusReserva.EXPIRADA)).status()));
    }
}
