package brenner.edu.reservas.core.usecase.exceptions;

import brenner.edu.reservas.core.domain.valueObjects.SalaId;

/** A sala pedida não existe — 404. */
public class SalaNotFoundException extends NotFoundException {

    public SalaNotFoundException(SalaId id) {
        super("Sala não encontrada: " + id.value());
    }
}
