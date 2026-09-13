package brenner.edu.reservas.output.persistence;

import brenner.edu.reservas.output.persistence.entities.SalaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Acesso Spring Data à tabela {@code sala}. Detalhe de infraestrutura: o core só enxerga os ports. */
public interface SalaRepository extends JpaRepository<SalaEntity, UUID> {
}
