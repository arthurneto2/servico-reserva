package brenner.edu.reservas.core.domain.valueObjects;

public record QuantidadeParticipantes(int value) {

    public QuantidadeParticipantes{
        if (value <= 0)
            throw new IllegalArgumentException("Quantidade de participantes deve ser maior que zero");
    }

    public boolean excede(Capacidade capacidade){
        return value > capacidade.value();
    }

}
