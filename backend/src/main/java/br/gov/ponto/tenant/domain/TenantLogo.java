package br.gov.ponto.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Logo do ente armazenada no próprio banco (alternativa ao S3 sem depender de infra externa,
 * 12.2.1). Um logo por tenant (PK = tenant_id). Servido por endpoint público por slug; a URL de
 * serviço é gravada no {@code Branding.logoUrl}, então o white-label existente o exibe sem mudança.
 */
@Entity
@Table(name = "tenant_logo")
public class TenantLogo {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected TenantLogo() {
    }

    public TenantLogo(UUID tenantId, String contentType, byte[] conteudo) {
        this.tenantId = tenantId;
        this.contentType = contentType;
        this.conteudo = conteudo;
    }

    public void atualizar(String contentType, byte[] conteudo) {
        this.contentType = contentType;
        this.conteudo = conteudo;
    }

    @PrePersist
    @PreUpdate
    void carimbar() {
        atualizadoEm = Instant.now();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getConteudo() {
        return conteudo;
    }
}
