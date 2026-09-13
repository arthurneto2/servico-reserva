package brenner.edu.reservas.core.domain.valueObjects;

import brenner.edu.reservas.core.domain.exceptions.IdentificadorInvalidoException;

import java.util.UUID;

public record SalaId (UUID value){

    public SalaId {
        if (value == null) {
            throw new IdentificadorInvalidoException("O identificador da sala não pode ser nulo");
        }
    }

    public static SalaId novo(){
        return new SalaId(UUID.randomUUID());
    }
}
