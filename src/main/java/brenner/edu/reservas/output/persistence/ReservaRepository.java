package brenner.edu.reservas.output.persistence;

import brenner.edu.reservas.output.persistence.entities.ReservaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Acesso Spring Data à tabela {@code reserva}. */
public interface ReservaRepository extends JpaRepository<ReservaEntity, UUID> {

    /**
     * Reservas da sala, nos status pedidos, que tocam a janela {@code [inicio, fim)}.
     *
     * <p>"Tocar" é começar antes de {@code fim} e terminar depois de {@code inicio}: reservas que
     * apenas encostam nas bordas — uma termina exatamente quando a janela começa — não entram,
     * porque também não conflitam.
     */
    @Query("""
            select r from ReservaEntity r
            where r.salaId = :salaId
              and r.status in :status
              and r.inicio < :fim
              and r.fim > :inicio
            order by r.inicio
            """)
    List<ReservaEntity> buscarNaJanela(@Param("salaId") UUID salaId,
                                       @Param("status") Collection<String> status,
                                       @Param("inicio") Instant inicio,
                                       @Param("fim") Instant fim);
}
