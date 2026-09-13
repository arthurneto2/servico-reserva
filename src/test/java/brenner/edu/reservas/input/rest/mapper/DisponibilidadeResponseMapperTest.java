package brenner.edu.reservas.input.rest.mapper;

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
import brenner.edu.reservas.core.usecase.results.DisponibilidadeDaSala;
import brenner.edu.reservas.input.rest.dto.DisponibilidadeResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisponibilidadeResponseMapperTest {

    private static final FusoHorario SAO_PAULO = FusoHorario.of("America/Sao_Paulo");
    private static final UUID SALA_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESERVA_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final SalaId SALA_ID = new SalaId(SALA_UUID);
    private static final LocalDate DIA = LocalDate.of(2026, 9, 15);
    private static final Sala SALA =
            new Sala(SALA_ID, "Sala Pirapora", new Capacidade(8), SAO_PAULO, true);

    private final DisponibilidadeResponseMapper mapper = new DisponibilidadeResponseMapper();

    private static DataHora em(String local) {
        return new DataHora(LocalDateTime.parse(local).atZone(SAO_PAULO.value()).toInstant(), SAO_PAULO);
    }

    private static Reserva reserva() {
        return Reserva.reconstituir(new ReservaId(RESERVA_UUID), SALA_ID, new Email("ana@empresa.com"),
                new Periodo(em("2026-09-15T14:00"), em("2026-09-15T15:30")),
                new QuantidadeParticipantes(6), StatusReserva.PENDENTE, em("2026-09-14T10:00"));
    }

    private static DisponibilidadeDaSala disponibilidade() {
        return new DisponibilidadeDaSala(SALA, DIA,
                List.of(new JanelaLivre(em("2026-09-15T08:00"), em("2026-09-15T14:00")),
                        new JanelaLivre(em("2026-09-15T15:30"), em("2026-09-15T20:00"))),
                List.of(reserva()));
    }

    @Test
    void mapeiaOsDadosDaSalaEODia() {
        DisponibilidadeResponse response = mapper.toResponse(disponibilidade());

        assertAll(
                () -> assertEquals(SALA_UUID, response.salaId()),
                () -> assertEquals("Sala Pirapora", response.nome()),
                () -> assertEquals("America/Sao_Paulo", response.fuso()),
                () -> assertEquals(DIA, response.data()));
    }

    @Test
    void mapeiaAsJanelasLivresNaOrdem() {
        DisponibilidadeResponse response = mapper.toResponse(disponibilidade());

        assertAll(
                () -> assertEquals(2, response.horariosLivres().size()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 8, 0),
                        response.horariosLivres().get(0).inicio()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0),
                        response.horariosLivres().get(0).fim()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 15, 30),
                        response.horariosLivres().get(1).inicio()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 20, 0),
                        response.horariosLivres().get(1).fim()));
    }

    @Test
    void resumeCadaReservaQueOcupaODia() {
        DisponibilidadeResponse response = mapper.toResponse(disponibilidade());

        assertAll(
                () -> assertEquals(1, response.reservas().size()),
                () -> assertEquals(RESERVA_UUID, response.reservas().get(0).id()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 14, 0), response.reservas().get(0).inicio()),
                () -> assertEquals(LocalDateTime.of(2026, 9, 15, 15, 30), response.reservas().get(0).fim()),
                () -> assertEquals("PENDENTE", response.reservas().get(0).status()));
    }

    @Test
    void diaLivreTemUmaJanelaENenhumaReserva() {
        DisponibilidadeDaSala diaLivre = new DisponibilidadeDaSala(SALA, DIA,
                List.of(new JanelaLivre(em("2026-09-15T08:00"), em("2026-09-15T20:00"))), List.of());

        DisponibilidadeResponse response = mapper.toResponse(diaLivre);

        assertAll(
                () -> assertEquals(1, response.horariosLivres().size()),
                () -> assertTrue(response.reservas().isEmpty()));
    }

    @Test
    void diaTodoOcupadoNaoTemJanelaAlguma() {
        DisponibilidadeDaSala lotado = new DisponibilidadeDaSala(SALA, DIA, List.of(), List.of(reserva()));

        DisponibilidadeResponse response = mapper.toResponse(lotado);

        assertAll(
                () -> assertTrue(response.horariosLivres().isEmpty()),
                () -> assertEquals(1, response.reservas().size()));
    }
}
