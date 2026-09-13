package brenner.edu.reservas.core.domain.valueObjects;

public record Capacidade(int value) {
    public Capacidade{
        if (value <= 0)
            throw new IllegalArgumentException("Capacidade deve ser maior que zero");
    }
}
