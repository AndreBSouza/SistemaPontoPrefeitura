package br.gov.ponto.lgpd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Registro de consentimento do titular para uma finalidade (ex.: BIOMETRIA). */
@Entity
@Table(name = "consentimento")
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "servidor_id", nullable = false)
    private UUID servidorId;

    @Column(nullable = false, length = 40)
    private String finalidade;

    @Column(nullable = false)
    private boolean concedido;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private Instant registradoEm;

    protected Consentimento() {
    }

    public Consentimento(UUID tenantId, UUID servidorId, String finalidade, boolean concedido) {
        this.tenantId = tenantId;
        this.servidorId = servidorId;
        this.finalidade = finalidade;
        this.concedido = concedido;
    }

    @PrePersist
    void prePersist() {
        if (registradoEm == null) {
            registradoEm = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getServidorId() {
        return servidorId;
    }

    public String getFinalidade() {
        return finalidade;
    }

    public boolean isConcedido() {
        return concedido;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }
}
