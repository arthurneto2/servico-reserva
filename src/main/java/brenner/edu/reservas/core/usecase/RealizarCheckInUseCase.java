package brenner.edu.reservas.core.usecase;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.exceptions.ReservaExpiradaException;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.port.GetDataHoraAtualPort;
import brenner.edu.reservas.core.port.GetReservaPort;
import brenner.edu.reservas.core.port.GetSalaPort;
import brenner.edu.reservas.core.port.SaveReservaPort;
import brenner.edu.reservas.core.usecase.commands.RealizarCheckInCommand;
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
 * Registra a presença do organizador, confirmando a reserva —
 * {@code POST /reservas/{id}/check-in}.
 *
 * <p>Quem decide é {@link Reserva#realizarCheckIn}: status, janela de 10 minutos antes e 15
 * minutos depois do início. O UseCase carrega a reserva, a sala (pelo fuso) e o "agora" nesse
 * fuso, e grava o resultado.
 *
 * <h2>O caso do check-in atrasado</h2>
 *
 * <p>Chegar depois da janela é a única situação do projeto em que o domínio <b>muda o estado e
 * falha ao mesmo tempo</b>: a reserva vira {@code EXPIRADA} e uma {@link ReservaExpiradaException}
 * é lançada. Os dois efeitos são necessários — o organizador precisa do erro, e a agenda precisa
 * ser liberada —, o que exige duas providências que andam juntas:
 *
 * <ol>
 *   <li>gravar <b>antes</b> de propagar: o {@code catch} abaixo persiste a reserva já expirada e
 *       só então relança a exceção original;</li>
 *   <li>{@code noRollbackFor = ReservaExpiradaException.class}: sem isso o Spring desfaria a
 *       transação ao ver a exceção subir, e a gravação do passo 1 iria junto — o
 *       {@code catch} sozinho não resolve nada.</li>
 * </ol>
 *
 * <p>Se a própria gravação da expiração falhar, é a {@link SaveReservaUnavailableException} (503)
 * que sobe, e não a expiração (422): não dá para anunciar como definitivo um estado que não foi
 * persistido — o cliente que repetir a chamada tem de encontrar a reserva ainda {@code PENDENTE}.
 *
 * <p>Alternativas consideradas: devolver um resultado em vez de lançar (perde o 422 natural do
 * handler e espalha {@code if} por toda a cadeia), ou expirar em um processo separado
 * (a reserva ficaria ocupando a agenda entre a tentativa e a varredura).
 */
public class RealizarCheckInUseCase {

    private final GetReservaPort getReservaPort;
    private final GetSalaPort getSalaPort;
    private final GetDataHoraAtualPort getDataHoraAtualPort;
    private final SaveReservaPort saveReservaPort;

    public RealizarCheckInUseCase(GetReservaPort getReservaPort,
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
     * @return a reserva {@code CONFIRMADA} efetivamente gravada
     * @throws ReservaNotFoundException reserva inexistente (404)
     * @throws SalaNotFoundException    a sala da reserva não existe mais (404)
     * @throws ReservaExpiradaException check-in depois da janela — a reserva fica gravada como
     *                                  {@code EXPIRADA} (422)
     * @throws brenner.edu.reservas.core.domain.exceptions.DomainException demais regras — status
     *                                  ou check-in antecipado (422)
     * @throws UnavailableException     qualquer port indisponível (503)
     */
    @Transactional(noRollbackFor = ReservaExpiradaException.class)
    public Reserva execute(RealizarCheckInCommand command) {
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

        try {
            reserva.realizarCheckIn(agora);
        } catch (ReservaExpiradaException expirada) {
            // A reserva já está EXPIRADA neste ponto; gravar é o que libera a agenda.
            gravar(reserva);
            throw expirada;
        }

        return gravar(reserva);
    }

    private Sala carregarSalaDaReserva(Reserva reserva) {
        return getSalaPort.getById(reserva.salaId())
                .getAvailableValueOrElseThrow(GetSalaUnavailableException::new)
                .orElseThrow(() -> new SalaNotFoundException(reserva.salaId()));
    }

    private Reserva gravar(Reserva reserva) {
        return saveReservaPort.save(reserva)
                .getAvailableValueOrElseThrow(SaveReservaUnavailableException::new)
                .orElseThrow(() -> new SaveReservaUnavailableException(new IllegalStateException(
                        "gravação devolveu vazio para a reserva " + reserva.id().value())));
    }
}
