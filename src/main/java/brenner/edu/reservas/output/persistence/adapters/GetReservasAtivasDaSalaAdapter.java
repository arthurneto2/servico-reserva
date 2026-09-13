package brenner.edu.reservas.output.persistence.adapters;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.port.GetReservasAtivasDaSalaPort;
import brenner.edu.reservas.output.persistence.ReservaRepository;
import brenner.edu.reservas.output.persistence.SalaRepository;
import brenner.edu.reservas.output.persistence.mappers.ReservaMapper;
import brenner.edu.reservas.output.persistence.mappers.SalaMapper;
import brenner.edu.reservas.shared.Result;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA de {@link GetReservasAtivasDaSalaPort}.
 *
 * <p>Quem define o que é "ativa" é este adapter, como manda o contrato do port: {@code PENDENTE}
 * ou {@code CONFIRMADA}. Quem chama pede a agenda de uma janela e não precisa saber que existem
 * outros status.
 *
 * <p>Agenda livre devolve {@code Result.of(List.of())}, e não {@code Result.empty()}: uma lista
 * vazia é uma resposta, não uma ausência de resposta.
 *
 * <p>O fuso vem da sala, e não do {@link DataHora} recebido: o port não promete que a janela
 * chega no fuso da sala, e reconstruir o {@code Periodo} no fuso errado faria uma reserva válida
 * parecer fora do horário de funcionamento.
 */
@Component
public class GetReservasAtivasDaSalaAdapter implements GetReservasAtivasDaSalaPort {

    private static final List<String> STATUS_ATIVOS =
            List.of(StatusReserva.PENDENTE.name(), StatusReserva.CONFIRMADA.name());

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final ReservaMapper reservaMapper;
    private final SalaMapper salaMapper;

    public GetReservasAtivasDaSalaAdapter(ReservaRepository reservaRepository,
                                          SalaRepository salaRepository,
                                          ReservaMapper reservaMapper,
                                          SalaMapper salaMapper) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.reservaMapper = reservaMapper;
        this.salaMapper = salaMapper;
    }

    @Override
    public Result<List<Reserva>> get(SalaId id, DataHora inicio, DataHora fim) {
        try {
            FusoHorario fuso = salaRepository.findById(id.value())
                    .map(salaMapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException("Sala inexistente: " + id.value()))
                    .fuso();

            List<Reserva> reservas = reservaRepository
                    .buscarNaJanela(id.value(), STATUS_ATIVOS, inicio.instante(), fim.instante())
                    .stream()
                    .map(entity -> reservaMapper.toDomain(entity, fuso))
                    .toList();

            return Result.of(reservas);
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
}
