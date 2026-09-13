package brenner.edu.reservas.input.rest.mapper;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.Sala;
import brenner.edu.reservas.core.domain.valueObjects.JanelaLivre;
import brenner.edu.reservas.core.usecase.results.DisponibilidadeDaSala;
import brenner.edu.reservas.input.rest.dto.DisponibilidadeResponse;
import brenner.edu.reservas.input.rest.dto.JanelaLivreResponse;
import brenner.edu.reservas.input.rest.dto.ReservaResumoResponse;
import org.springframework.stereotype.Component;

import static brenner.edu.reservas.input.rest.mapper.ReservaResponseMapper.local;

/**
 * Converte o resultado {@link DisponibilidadeDaSala} em {@link DisponibilidadeResponse}.
 *
 * <p>O fuso aparece <b>uma vez</b>, no topo da resposta: todas as datas abaixo dele são locais a
 * esse fuso, e repeti-lo em cada janela e em cada reserva só engordaria o JSON.
 *
 * <p>As reservas viram resumo — id, horário e status. Organizador e número de participantes ficam
 * de fora porque a pergunta aqui é "o que ocupa a agenda", e não "quem reservou".
 */
@Component
public class DisponibilidadeResponseMapper {

    public DisponibilidadeResponse toResponse(DisponibilidadeDaSala disponibilidade) {
        Sala sala = disponibilidade.sala();

        return new DisponibilidadeResponse(sala.id().value(),
                sala.nome(),
                sala.fuso().value().getId(),
                disponibilidade.data(),
                disponibilidade.horariosLivres().stream().map(this::toResponse).toList(),
                disponibilidade.reservas().stream().map(this::toResumo).toList());
    }

    private JanelaLivreResponse toResponse(JanelaLivre janela) {
        return new JanelaLivreResponse(local(janela.inicio()), local(janela.fim()));
    }

    private ReservaResumoResponse toResumo(Reserva reserva) {
        return new ReservaResumoResponse(reserva.id().value(),
                local(reserva.periodo().inicio()),
                local(reserva.periodo().fim()),
                reserva.status().name());
    }
}
