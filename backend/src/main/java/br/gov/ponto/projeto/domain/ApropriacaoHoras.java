package br.gov.ponto.projeto.domain;

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

/** Apropriação de horas de um vínculo a um projeto/convênio (apoio à prestação de contas). */
@Entity
@Table(name = "apropriacao_horas")
public class ApropriacaoHoras {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(name = "projeto_id", nullable = false)
    private UUID projetoId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private int minutos;

    @Column(length = 300)
    private String descricao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected ApropriacaoHoras() {
    }

    public ApropriacaoHoras(UUID tenantId, UUID vinculoId, UUID projetoId,
                            LocalDate data, int minutos, String descricao) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.projetoId = projetoId;
        this.data = data;
        this.minutos = minutos;
        this.descricao = descricao;
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

    public UUID getVinculoId() {
        return vinculoId;
    }

    public UUID getProjetoId() {
        return projetoId;
    }

    public LocalDate getData() {
        return data;
    }

    public int getMinutos() {
        return minutos;
    }

    public String getDescricao() {
        return descricao;
    }
}
