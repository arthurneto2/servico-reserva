package brenner.edu.reservas.core.usecase;

import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.exceptions.DataNoPassadoException;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservasAtivasDaSalaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.usecase.commands.ConsultarDisponibilidadeCommand;
import brenner.edu.reservas.core.usecase.exceptions.GetDataHoraAtualUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetReservasAtivasDaSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.SalaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.UnavailableException;
import brenner.edu.reservas.core.usecase.results.DisponibilidadeDaSala;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Lista os horários livres de uma sala em um dia —
 * {@code GET /salas/{id}/disponibilidade?data=...}.
 *
 * <p>Uma consulta também é um UseCase: recebe um Command, passa pelos mesmos ports e trata os
 * mesmos três estados do {@code Result}. Não existe atalho "só leitura" que vá direto ao
 * repositório — fosse assim, o dia em que a agenda vier de outro serviço, o hexágono já estaria
 * furado.
 *
 * <p>O cálculo em si é do domínio ({@link Sala#horariosLivresEm}); aqui se decide apenas o que
 * perguntar: a sala, o "agora" no fuso dela e as reservas que ocupam o dia pedido.
 */
public class ConsultarDisponibilidadeUseCase {

    private final GetSalaPort getSalaPort;
    private final GetDataHoraAtualPort getDataHoraAtualPort;
    private final GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort;

    public ConsultarDisponibilidadeUseCase(GetSalaPort getSalaPort,
                                           GetDataHoraAtualPort getDataHoraAtualPort,
                                           GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort) {
        this.getSalaPort = Objects.requireNonNull(getSalaPort, "getSalaPort é obrigatório");
        this.getDataHoraAtualPort =
                Objects.requireNonNull(getDataHoraAtualPort, "getDataHoraAtualPort é obrigatório");
        this.getReservasAtivasDaSalaPort = Objects.requireNonNull(
                getReservasAtivasDaSalaPort, "getReservasAtivasDaSalaPort é obrigatório");
    }

    /**
     * @return a sala, o dia consultado, as lacunas livres entre 08:00 e 20:00 e as reservas que
     *         ocupam esse dia
     * @throws SalaNotFoundException    sala inexistente (404)
     * @throws DataNoPassadoException   dia anterior ao de hoje no fuso da sala (422)
     * @throws UnavailableException     qualquer port indisponível (503)
     */
    @Transactional(readOnly = true)
    public DisponibilidadeDaSala execute(ConsultarDisponibilidadeCommand command) {
        Objects.requireNonNull(command, "command é obrigatório");

        final var salaId = new SalaId(command.salaId());
        final var sala = getSalaPort.getById(salaId)
                .getAvailableValueOrElseThrow(GetSalaUnavailableException::new)
                .orElseThrow(() -> new SalaNotFoundException(salaId));

        final var agora = getDataHoraAtualPort.get(sala.fuso())
                .getAvailableValueOrElseThrow(GetDataHoraAtualUnavailableException::new)
                .orElseThrow(() -> new GetDataHoraAtualUnavailableException(new IllegalStateException(
                        "relógio devolveu vazio para o fuso " + sala.fuso().value())));

        final var data = command.data();
        // "Hoje" é o da sala: às 23h em São Paulo, uma sala em Lisboa já virou o dia, e consultar
        // o dia seguinte não é consultar o futuro.
        if (data.isBefore(agora.data()))
            throw new DataNoPassadoException("Data " + data + " já passou; hoje, em "
                    + sala.fuso().value() + ", é " + agora.data());

        final var reservas = getReservasAtivasDaSalaPort
                .get(salaId, inicioDoDia(data, sala.fuso()), inicioDoDia(data.plusDays(1), sala.fuso()))
                .getAvailableValueOrElseThrow(GetReservasAtivasDaSalaUnavailableException::new)
                .orElseGet(List::of);

        return new DisponibilidadeDaSala(sala, data, sala.horariosLivresEm(data, reservas), reservas);
    }

    private static DataHora inicioDoDia(LocalDate dia, FusoHorario fuso) {
        return new DataHora(dia.atStartOfDay(fuso.value()).toInstant(), fuso);
    }
}
