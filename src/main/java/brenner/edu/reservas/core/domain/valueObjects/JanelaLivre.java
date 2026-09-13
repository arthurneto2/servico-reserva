package brenner.edu.reservas.core.domain.valueObjects;

import brenner.edu.reservas.core.domain.exceptions.JanelaLivreInvalidaException;

import java.time.Duration;

/**
 * Uma lacuna livre na agenda de uma sala.
 *
 * <p>Diferente de {@link Periodo}, não carrega as regras de reserva (duração mínima/máxima,
 * alinhamento de 15 minutos): uma lacuna é o que sobrou da agenda, de qualquer tamanho.
 * Cabe a quem consome escolher um {@link Periodo} válido dentro dela.
 */
public record JanelaLivre(DataHora inicio, DataHora fim) {

    public JanelaLivre {
        if (inicio == null)
            throw new JanelaLivreInvalidaException("Início não pode ser nulo");
        if (fim == null)
            throw new JanelaLivreInvalidaException("Fim não pode ser nulo");

        if (!inicio.fusoHorario().equals(fim.fusoHorario()))
            throw new JanelaLivreInvalidaException(
                    "Início e fim devem estar no mesmo fuso horário: "
                            + inicio.fusoHorario().value() + " e " + fim.fusoHorario().value());

        if (!inicio.antesDe(fim))
            throw new JanelaLivreInvalidaException("Fim deve ser estritamente depois do início");
    }

    public Duration duracao() {
        return Duration.between(inicio.instante(), fim.instante());
    }

    /** Indica se o período cabe inteiro dentro desta janela. */
    public boolean comporta(Periodo periodo) {
        if (periodo == null)
            throw new JanelaLivreInvalidaException("Período comparado não pode ser nulo");

        return !periodo.inicio().antesDe(inicio) && !periodo.fim().depoisDe(fim);
    }
}
