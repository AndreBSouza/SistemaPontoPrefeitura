package br.gov.ponto.ativacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Codigo de ativacao de uso unico, gerado pelo RH para um vinculo. Guardamos apenas
 * o hash; o codigo legivel so existe no momento da geracao. Sem RLS: a consulta na
 * ativacao ocorre antes de haver tenant no contexto (resolve o tenant pelo proprio codigo).
 */
@Entity
@Table(name = "codigo_ativacao")
public class CodigoAtivacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(name = "codigo_hash", nullable = false, length = 64)
    private String codigoHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected CodigoAtivacao() {
    }

    public CodigoAtivacao(UUID tenantId, UUID vinculoId, String codigoHash, Instant expiraEm) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.codigoHash = codigoHash;
        this.expiraEm = expiraEm;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public boolean valido(Instant agora) {
        return !usado && expiraEm.isAfter(agora);
    }

    public void marcarUsado() {
        this.usado = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getVinculoId() {
        return vinculoId;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public boolean isUsado() {
        return usado;
    }
}
