package brenner.edu.reservas.input.rest.mapper;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.input.rest.dto.ReservaResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Converte o agregado {@link Reserva} em {@link ReservaResponse}.
 *
 * <p>Este pacote é o <b>único</b> ponto da camada de entrada que enxerga {@code core.domain}: o
 * controller recebe DTO e devolve DTO, e quem atravessa a fronteira é o mapper. Por isso a
 * tradução de vocabulário acontece aqui — Value Object vira tipo primitivo, {@code StatusReserva}
 * vira texto.
 *
 * <p>A conversão que importa é a de tempo: o domínio guarda um instante mais o fuso da sala, e a
 * resposta mostra a hora local desse fuso junto do fuso em si. Sem o segundo campo o primeiro
 * seria ambíguo — "14:00" de onde?
 */
@Component
public class ReservaResponseMapper {

    public ReservaResponse toResponse(Reserva reserva) {
        DataHora inicio = reserva.periodo().inicio();

        return new ReservaResponse(reserva.id().value(),
                reserva.salaId().value(),
                reserva.organizador().value(),
                local(inicio),
                local(reserva.periodo().fim()),
                inicio.fusoHorario().value().getId(),
                reserva.participantes().value(),
                reserva.status().name(),
                local(reserva.criadaEm()));
    }

    static LocalDateTime local(DataHora dataHora) {
        return LocalDateTime.of(dataHora.data(), dataHora.hora());
    }
}
