package brenner.edu.reservas.core.usecase.results;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.valueObjects.JanelaLivre;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A agenda de uma sala em um dia: o que sobrou livre e o que já está ocupado.
 *
 * <p>Existe porque a resposta do endpoint não cabe em um agregado só — ela combina dados da
 * {@link Sala} (nome e fuso), o dia consultado, as lacunas calculadas e as reservas que as
 * produziram. Carrega objetos de domínio, e não Strings: quem os traduz para JSON é o mapper da
 * camada {@code input}, o único lugar de lá que enxerga o domínio.
 *
 * <p>As duas listas são copiadas na entrada: o resultado de uma consulta não muda porque quem a
 * pediu — ou o adapter que a alimentou — mexeu na lista depois.
 */
public record DisponibilidadeDaSala(Sala sala,
                                    LocalDate data,
                                    List<JanelaLivre> horariosLivres,
                                    List<Reserva> reservas) {

    public DisponibilidadeDaSala {
        Objects.requireNonNull(sala, "sala é obrigatória");
        Objects.requireNonNull(data, "data é obrigatória");
        horariosLivres = List.copyOf(Objects.requireNonNull(horariosLivres, "horariosLivres é obrigatório"));
        reservas = List.copyOf(Objects.requireNonNull(reservas, "reservas é obrigatório"));
    }
}
