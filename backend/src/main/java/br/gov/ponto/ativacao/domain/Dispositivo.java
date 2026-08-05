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
 * Aparelho do servidor vinculado a um vinculo (login unico por dispositivo).
 * Guardamos o hash do token; o RH pode revogar (perda/troca de aparelho).
 * Sem RLS: a autenticacao por token resolve o tenant antes de haver contexto.
 */
@Entity
@Table(name = "dispositivo")
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(length = 120)
    private String nome;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Dispositivo() {
    }

    public Dispositivo(UUID tenantId, UUID vinculoId, String nome, String tokenHash) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.nome = nome;
        this.tokenHash = tokenHash;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public void revogar() {
        this.ativo = false;
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

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
