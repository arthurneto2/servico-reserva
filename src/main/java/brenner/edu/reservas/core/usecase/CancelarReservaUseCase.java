package brenner.edu.reservas.core.usecase;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.port.SaveReservaPort;
import brenner.edu.reservas.core.usecase.commands.CancelarReservaCommand;
import brenner.edu.reservas.core.usecase.exceptions.GetDataHoraAtualUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetReservaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.GetSalaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.ReservaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.SalaNotFoundException;
import brenner.edu.reservas.core.usecase.exceptions.SaveReservaUnavailableException;
import brenner.edu.reservas.core.usecase.exceptions.UnavailableException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Cancela uma reserva a pedido do organizador — {@code DELETE /reservas/{id}}.
 *
 * <p>Quem decide se o cancelamento é permitido é {@link Reserva#cancelar}: status, identidade do
 * solicitante e prazo de duas horas são regras de negócio, não orquestração. O UseCase reúne os
 * insumos — a reserva, a sala (só para saber em que fuso ela vive) e o "agora" nesse fuso — e
 * grava o resultado.
 *
 * <p>A sala é carregada mesmo sem participar da regra porque "agora" só tem sentido em um fuso, e
 * o fuso é da sala: cancelar às 13h em São Paulo e às 13h em Londres são instantes diferentes.
 *
 * <p>Não devolve nada: o endpoint responde {@code 204 No Content}.
 */
public class CancelarReservaUseCase {

    private final GetReservaPort getReservaPort;
    private final GetSalaPort getSalaPort;
    private final GetDataHoraAtualPort getDataHoraAtualPort;
    private final SaveReservaPort saveReservaPort;

    public CancelarReservaUseCase(GetReservaPort getReservaPort,
                                  GetSalaPort getSalaPort,
                                  GetDataHoraAtualPort getDataHoraAtualPort,
                                  SaveReservaPort saveReservaPort) {
        this.getReservaPort = Objects.requireNonNull(getReservaPort, "getReservaPort é obrigatório");
        this.getSalaPort = Objects.requireNonNull(getSalaPort, "getSalaPort é obrigatório");
        this.getDataHoraAtualPort =
                Objects.requireNonNull(getDataHoraAtualPort, "getDataHoraAtualPort é obrigatório");
        this.saveReservaPort = Objects.requireNonNull(saveReservaPort, "saveReservaPort é obrigatório");
    }

    /**
     * @throws ReservaNotFoundException reserva inexistente (404)
     * @throws SalaNotFoundException    a sala da reserva não existe mais (404)
     * @throws brenner.edu.reservas.core.domain.exceptions.DomainException regra de negócio
     *                                  violada — status, solicitante ou prazo (422)
     * @throws UnavailableException     qualquer port indisponível (503)
     */
    @Transactional
    public void execute(CancelarReservaCommand command) {
        Objects.requireNonNull(command, "command é obrigatório");

        final var reservaId = new ReservaId(command.reservaId());
        final var reserva = getReservaPort.getById(reservaId)
                .getAvailableValueOrElseThrow(GetReservaUnavailableException::new)
                .orElseThrow(() -> new ReservaNotFoundException(reservaId));

        final var sala = carregarSalaDaReserva(reserva);

        final var agora = getDataHoraAtualPort.get(sala.fuso())
                .getAvailableValueOrElseThrow(GetDataHoraAtualUnavailableException::new)
                .orElseThrow(() -> new GetDataHoraAtualUnavailableException(new IllegalStateException(
                        "relógio devolveu vazio para o fuso " + sala.fuso().value())));

        reserva.cancelar(new Email(command.solicitante()), agora);

        saveReservaPort.save(reserva)
                .getAvailableValueOrElseThrow(SaveReservaUnavailableException::new)
                .orElseThrow(() -> new SaveReservaUnavailableException(new IllegalStateException(
                        "gravação devolveu vazio para a reserva " + reserva.id().value())));
    }

    private Sala carregarSalaDaReserva(Reserva reserva) {
        return getSalaPort.getById(reserva.salaId())
                .getAvailableValueOrElseThrow(GetSalaUnavailableException::new)
                .orElseThrow(() -> new SalaNotFoundException(reserva.salaId()));
    }
}
