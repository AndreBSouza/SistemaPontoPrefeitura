package br.gov.ponto.saas.domain;

import br.gov.ponto.tenant.domain.TipoPoder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Solicitação pública de adesão de um ente (self-service onboarding, 12.3.13). É PRÉ-tenant:
 * fica numa tabela global (sem RLS, como {@code tenant}) e só vira um tenant de fato após
 * aprovação manual do operador da plataforma — evita criação pública indiscriminada de entes.
 */
@Entity
@Table(name = "solicitacao_ente")
public class SolicitacaoEnte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 60)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_poder", nullable = false, length = 20)
    private TipoPoder tipoPoder;

    @Column(name = "responsavel_nome", nullable = false, length = 120)
    private String responsavelNome;

    @Column(name = "responsavel_email", nullable = false, length = 160)
    private String responsavelEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSolicitacao status;

    /** Tenant provisionado quando aprovada. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "motivo_decisao", length = 300)
    private String motivoDecisao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "decidido_em")
    private Instant decididoEm;

    protected SolicitacaoEnte() {
    }

    public SolicitacaoEnte(String nome, String slug, TipoPoder tipoPoder,
                           String responsavelNome, String responsavelEmail) {
        this.nome = nome;
        this.slug = slug;
        this.tipoPoder = tipoPoder;
        this.responsavelNome = responsavelNome;
        this.responsavelEmail = responsavelEmail;
        this.status = StatusSolicitacao.PENDENTE;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public void aprovar(UUID tenantId) {
        this.status = StatusSolicitacao.APROVADA;
        this.tenantId = tenantId;
        this.decididoEm = Instant.now();
    }

    public void rejeitar(String motivo) {
        this.status = StatusSolicitacao.REJEITADA;
        this.motivoDecisao = motivo;
        this.decididoEm = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSlug() {
        return slug;
    }

    public TipoPoder getTipoPoder() {
        return tipoPoder;
    }

    public String getResponsavelNome() {
        return responsavelNome;
    }

    public String getResponsavelEmail() {
        return responsavelEmail;
    }

    public StatusSolicitacao getStatus() {
        return status;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getMotivoDecisao() {
        return motivoDecisao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getDecididoEm() {
        return decididoEm;
    }
}
