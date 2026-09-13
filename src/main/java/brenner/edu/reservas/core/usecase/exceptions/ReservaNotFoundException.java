package brenner.edu.reservas.core.usecase.exceptions;

import brenner.edu.reservas.core.domain.valueObjects.ReservaId;

/** A reserva pedida não existe — 404. */
public class ReservaNotFoundException extends NotFoundException {

    public ReservaNotFoundException(ReservaId id) {
        super("Reserva não encontrada: " + id.value());
    }
}
