package brenner.edu.reservas.output.notification;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.port.NotificarOrganizadorPort;
import brenner.edu.reservas.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter de {@link NotificarOrganizadorPort}. Neste projeto a "notificação" é uma linha de log,
 * mas o contrato é o de uma integração de verdade: pode falhar, e falha vira
 * {@code Result.failure(e)} — nunca exceção solta.
 *
 * <p>Como é chamado dentro da transação do UseCase, uma falha aqui desfaz a reserva. Trocado por
 * um envio de e-mail real, esse detalhe passaria a importar muito: valeria mover o envio para
 * depois do commit, aceitando que uma reserva gravada possa ficar sem aviso.
 */
@Component
public class NotificarOrganizadorAdapter implements NotificarOrganizadorPort {

    private static final Logger log = LoggerFactory.getLogger(NotificarOrganizadorAdapter.class);

    @Override
    public Result<Void> notificar(Reserva reserva) {
        try {
            log.info("Notificando {} sobre a reserva {} na sala {}: {} a {} ({}), status {}",
                    reserva.organizador().value(),
                    reserva.id().value(),
                    reserva.salaId().value(),
                    reserva.periodo().inicio().hora(),
                    reserva.periodo().fim().hora(),
                    reserva.periodo().inicio().fusoHorario().value(),
                    reserva.status());
            return Result.empty();
        } catch (Exception e) {
            return Result.failure(e);
        }
    }
}
