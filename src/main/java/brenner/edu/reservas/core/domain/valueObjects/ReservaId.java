package brenner.edu.reservas.core.domain.valueObjects;

import brenner.edu.reservas.core.domain.exceptions.IdentificadorInvalidoException;

import java.util.UUID;

public record ReservaId(UUID value) {

    public ReservaId {
        if (value == null) {
            throw new IdentificadorInvalidoException("O identificador da reserva não pode ser nulo");
        }
    }

    public static ReservaId novo(){
        return new ReservaId(UUID.randomUUID());
    }
}
