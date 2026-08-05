package br.gov.ponto.delegacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Delegação de aprovação (12.6.8): no período [dataInicio, dataFim], o {@code delegado}
 * (substituto) passa a ver e decidir as pendências de chefia do {@code delegante} (gestor
 * titular, ex.: durante as férias dele).
 */
@Entity
@Table(name = "delegacao_aprovacao")
public class Delegacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Gestor titular que delega a aprovação. */
    @Column(name = "delegante_servidor_id", nullable = false)
    private UUID deleganteServidorId;

    /** Substituto que recebe a aprovação no período. */
    @Column(name = "delegado_servidor_id", nullable = false)
    private UUID delegadoServidorId;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Delegacao() {
    }

    public Delegacao(UUID tenantId, UUID deleganteServidorId, UUID delegadoServidorId,
                     LocalDate dataInicio, LocalDate dataFim) {
        this.tenantId = tenantId;
        this.deleganteServidorId = deleganteServidorId;
        this.delegadoServidorId = delegadoServidorId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
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

    public UUID getDeleganteServidorId() {
        return deleganteServidorId;
    }

    public UUID getDelegadoServidorId() {
        return delegadoServidorId;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
