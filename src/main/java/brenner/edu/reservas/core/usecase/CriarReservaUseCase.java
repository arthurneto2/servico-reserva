package brenner.edu.reservas.core.usecase;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservasAtivasDaSalaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.port.NotificarOrganizadorPort;
import brenner.edu.reservas.core.port.SaveReservaPort;
import brenner.edu.reservas.core.usecase.commands.CriarReservaCommand;
import brenner.edu.reservas.core.usecase.exceptions.GetDataHoraAtualUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetReservasAtivasDaSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.NotificarOrganizadorUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.SalaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.SaveReservaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.UnavailableException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Cria uma reserva para uma sala — {@code POST /reservas}.
 *
 * <p>Orquestra ports e domínio, sem conter regra de negócio: quem decide se a reserva pode
 * existir é {@link Sala#reservar}. O papel deste UseCase é reunir os insumos dessa decisão
 * (a sala, o "agora" no fuso dela, a agenda do dia) e, depois, persistir e notificar.
 *
 * <p>Os horários chegam no Command como {@link LocalDateTime} <b>local ao fuso da sala</b>: o
 * controller não sabe em que fuso a sala vive, então a conversão para {@link DataHora} só é
 * possível aqui, depois de carregar a sala.
 *
 * <p>Tudo roda em uma transação: se a notificação falhar, a reserva gravada é revertida —
 * uma reserva que o organizador nunca soube que existe ocuparia a agenda em silêncio.
 */
public class CriarReservaUseCase {

    private final GetSalaPort getSalaPort;
    private final GetDataHoraAtualPort getDataHoraAtualPort;
    private final GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort;
    private final SaveReservaPort saveReservaPort;
    private final NotificarOrganizadorPort notificarOrganizadorPort;

    public CriarReservaUseCase(GetSalaPort getSalaPort,
                               GetDataHoraAtualPort getDataHoraAtualPort,
                               GetReservasAtivasDaSalaPort getReservasAtivasDaSalaPort,
                               SaveReservaPort saveReservaPort,
                               NotificarOrganizadorPort notificarOrganizadorPort) {
        this.getSalaPort = Objects.requireNonNull(getSalaPort, "getSalaPort é obrigatório");
        this.getDataHoraAtualPort =
                Objects.requireNonNull(getDataHoraAtualPort, "getDataHoraAtualPort é obrigatório");
        this.getReservasAtivasDaSalaPort = Objects.requireNonNull(
                getReservasAtivasDaSalaPort, "getReservasAtivasDaSalaPort é obrigatório");
        this.saveReservaPort = Objects.requireNonNull(saveReservaPort, "saveReservaPort é obrigatório");
        this.notificarOrganizadorPort = Objects.requireNonNull(
                notificarOrganizadorPort, "notificarOrganizadorPort é obrigatório");
    }

    /**
     * @return a reserva {@code PENDENTE} efetivamente gravada
     * @throws SalaNotFoundException                        sala inexistente (404)
     * @throws brenner.edu.reservas.core.domain.exceptions.DomainException regra de negócio
     *                                                      violada — sala inativa, capacidade,
     *                                                      antecedência, conflito, período ou
     *                                                      e-mail inválido (422)
     * @throws UnavailableException                         qualquer port indisponível (503)
     */
    @Transactional
    public Reserva execute(CriarReservaCommand command) {
        Objects.requireNonNull(command, "command é obrigatório");

        final var salaId = new SalaId(command.salaId());
        final var sala = getSalaPort.getById(salaId)
                .getAvailableValueOrElseThrow(GetSalaUnavailableException::new)
                .orElseThrow(() -> new SalaNotFoundException(salaId));

        // O relógio é um port porque "agora" depende do fuso da sala; vazio aqui não é ausência
        // de dado, é um relógio quebrado — logo, 503 e não 404.
        final var agora = getDataHoraAtualPort.get(sala.fuso())
                .getAvailableValueOrElseThrow(GetDataHoraAtualUnavailableException::new)
                .orElseThrow(() -> new GetDataHoraAtualUnavailableException(new IllegalStateException(
                        "relógio devolveu vazio para o fuso " + sala.fuso().value())));

        final var periodo = new Periodo(emFusoDaSala(command.inicio(), sala.fuso()),
                emFusoDaSala(command.fim(), sala.fuso()));

        // A janela consultada é o dia local inteiro da sala: basta para detectar conflito, já que
        // Periodo garante que início e fim caem no mesmo dia local.
        final var dia = periodo.inicio().data();
        final var reservasAtivas = getReservasAtivasDaSalaPort
                .get(salaId, inicioDoDia(dia, sala.fuso()), inicioDoDia(dia.plusDays(1), sala.fuso()))
                .getAvailableValueOrElseThrow(GetReservasAtivasDaSalaUnavailableException::new)
                .orElseGet(List::of);

        final var reserva = sala.reservar(new Email(command.organizador()),
                periodo,
                new QuantidadeParticipantes(command.quantidadeParticipantes()),
                reservasAtivas,
                agora);

        final var gravada = saveReservaPort.save(reserva)
                .getAvailableValueOrElseThrow(SaveReservaUnavailableException::new)
                .orElseThrow(() -> new SaveReservaUnavailableException(new IllegalStateException(
                        "gravação devolveu vazio para a reserva " + reserva.id().value())));

        notificarOrganizadorPort.notificar(gravada)
                .getAvailableValueOrElseThrow(NotificarOrganizadorUnavailableException::new);

        return gravada;
    }

    private static DataHora emFusoDaSala(LocalDateTime local, FusoHorario fuso) {
        return new DataHora(local.atZone(fuso.value()).toInstant(), fuso);
    }

    private static DataHora inicioDoDia(LocalDate dia, FusoHorario fuso) {
        return new DataHora(dia.atStartOfDay(fuso.value()).toInstant(), fuso);
    }
}
