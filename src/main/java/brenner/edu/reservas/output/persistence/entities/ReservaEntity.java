package brenner.edu.reservas.output.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Espelho da tabela {@code reserva} do {@code docker/init.sql}.
 *
 * <p>Duas escolhas que valem explicação:
 * <ul>
 *   <li><b>{@code salaId} é um UUID solto, não um {@code @ManyToOne}</b>: a entidade é um espelho
 *       plano do DDL, e o agregado do domínio também guarda só o {@code SalaId} — carregar a sala
 *       junto seria decidir, na infraestrutura, uma navegação que o domínio não tem;</li>
 *   <li><b>{@code status} é {@code String}, não o enum do domínio</b>: mantém a entidade sem
 *       depender de {@code core.domain} e deixa a conversão — com a recusa de um valor
 *       desconhecido — no {@code ReservaMapper}, que é onde os outros Value Objects também
 *       nascem.</li>
 * </ul>
 *
 * <p>{@code inicio}, {@code fim} e {@code criadaEm} são {@code TIMESTAMPTZ} ↔ {@link Instant}: no
 * banco fica o instante, e a hora local é recalculada no fuso da sala na hora de mapear.
 */
@Entity
@Table(name = "reserva")
public class ReservaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "sala_id", nullable = false)
    private UUID salaId;

    @Column(name = "organizador_email", nullable = false, length = 254)
    private String organizadorEmail;

    @Column(name = "inicio", nullable = false)
    private Instant inicio;

    @Column(name = "fim", nullable = false)
    private Instant fim;

    @Column(name = "quantidade_participantes", nullable = false)
    private int quantidadeParticipantes;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSalaId() {
        return salaId;
    }

    public void setSalaId(UUID salaId) {
        this.salaId = salaId;
    }

    public String getOrganizadorEmail() {
        return organizadorEmail;
    }

    public void setOrganizadorEmail(String organizadorEmail) {
        this.organizadorEmail = organizadorEmail;
    }

    public Instant getInicio() {
        return inicio;
    }

    public void setInicio(Instant inicio) {
        this.inicio = inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public void setFim(Instant fim) {
        this.fim = fim;
    }

    public int getQuantidadeParticipantes() {
        return quantidadeParticipantes;
    }

    public void setQuantidadeParticipantes(int quantidadeParticipantes) {
        this.quantidadeParticipantes = quantidadeParticipantes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(Instant criadaEm) {
        this.criadaEm = criadaEm;
    }
}
