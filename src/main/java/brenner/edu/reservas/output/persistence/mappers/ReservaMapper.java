package brenner.edu.reservas.output.persistence.mappers;

import brenner.edu.reservas.core.domain.Reserva;
import brenner.edu.reservas.core.domain.StatusReserva;
import brenner.edu.reservas.core.domain.valueObjects.DataHora;
import brenner.edu.reservas.core.domain.valueObjects.Email;
import brenner.edu.reservas.core.domain.valueObjects.FusoHorario;
import brenner.edu.reservas.core.domain.valueObjects.Periodo;
import brenner.edu.reservas.core.domain.valueObjects.QuantidadeParticipantes;
import brenner.edu.reservas.core.domain.valueObjects.ReservaId;
import brenner.edu.reservas.core.domain.valueObjects.SalaId;
import brenner.edu.reservas.output.persistence.entities.ReservaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Converte {@link ReservaEntity} ↔ {@link Reserva}.
 *
 * <p>O fuso é um parâmetro, e não um campo da tabela: quem o conhece é a sala. Sem ele um
 * {@code Instant} não vira {@link DataHora}, e as invariantes de {@link Periodo} — janela
 * 08:00–20:00, mesmo dia local, alinhamento de 15 minutos — seriam avaliadas no fuso errado.
 * Cabe ao adapter carregar a sala antes de chamar este mapper.
 *
 * <p>Como em {@link SalaMapper}, reconstruir é reexecutar invariantes: dado torto no banco vira
 * exceção aqui e {@code Result.failure(e)} no adapter.
 */
@Component
public class ReservaMapper {

    public Reserva toDomain(ReservaEntity entity, FusoHorario fuso) {
        return Reserva.reconstituir(new ReservaId(entity.getId()),
                new SalaId(entity.getSalaId()),
                new Email(entity.getOrganizadorEmail()),
                new Periodo(em(entity.getInicio(), fuso), em(entity.getFim(), fuso)),
                new QuantidadeParticipantes(entity.getQuantidadeParticipantes()),
                StatusReserva.valueOf(entity.getStatus()),
                em(entity.getCriadaEm(), fuso));
    }

    public ReservaEntity toEntity(Reserva reserva) {
        ReservaEntity entity = new ReservaEntity();
        entity.setId(reserva.id().value());
        entity.setSalaId(reserva.salaId().value());
        entity.setOrganizadorEmail(reserva.organizador().value());
        entity.setInicio(reserva.periodo().inicio().instante());
        entity.setFim(reserva.periodo().fim().instante());
        entity.setQuantidadeParticipantes(reserva.participantes().value());
        entity.setStatus(reserva.status().name());
        entity.setCriadaEm(reserva.criadaEm().instante());
        return entity;
    }

    private static DataHora em(Instant instante, FusoHorario fuso) {
        return new DataHora(instante, fuso);
    }
}
