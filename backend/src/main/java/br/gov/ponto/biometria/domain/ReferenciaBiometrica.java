package br.gov.ponto.biometria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Referencia biometrica (template/hash) do servidor, cadastrada apos consentimento. */
@Entity
@Table(name = "referencia_biometrica")
public class ReferenciaBiometrica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "servidor_id", nullable = false)
    private UUID servidorId;

    /** Descritor facial (embedding) serializado em CSV — base da comparacao 1:1. */
    @Column(nullable = false, length = 8192)
    private String referencia;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected ReferenciaBiometrica() {
    }

    public ReferenciaBiometrica(UUID tenantId, UUID servidorId, String referencia) {
        this.tenantId = tenantId;
        this.servidorId = servidorId;
        this.referencia = referencia;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getServidorId() {
        return servidorId;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }
}
