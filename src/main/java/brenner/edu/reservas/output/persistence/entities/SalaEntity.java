package brenner.edu.reservas.output.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Espelho da tabela {@code sala} do {@code docker/init.sql}.
 *
 * <p>É uma estrutura de dados, e não um agregado: campos mutáveis, construtor vazio e nenhuma
 * invariante — quem garante as regras é {@code core.domain.Sala}, reconstruído pelo
 * {@code SalaMapper}. A aplicação sobe com {@code ddl-auto=validate}, então cada coluna aqui
 * precisa bater com o DDL.
 */
@Entity
@Table(name = "sala")
public class SalaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 100, unique = true)
    private String nome;

    @Column(name = "capacidade", nullable = false)
    private int capacidade;

    /** ID IANA, ex.: {@code America/Sao_Paulo}. */
    @Column(name = "fuso_horario", nullable = false, length = 60)
    private String fusoHorario;

    @Column(name = "ativa", nullable = false)
    private boolean ativa;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(int capacidade) {
        this.capacidade = capacidade;
    }

    public String getFusoHorario() {
        return fusoHorario;
    }

    public void setFusoHorario(String fusoHorario) {
        this.fusoHorario = fusoHorario;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
}
