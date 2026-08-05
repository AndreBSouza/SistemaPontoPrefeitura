package br.gov.ponto.ausencia.domain;

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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ausência programada de um servidor (férias, licença). No período [dataInicio, dataFim]
 * não se espera trabalho: a apuração trata os dias como não úteis (não gera falta).
 */
@Entity
@Table(name = "ausencia_programada")
public class AusenciaProgramada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TipoAusencia tipo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(length = 300)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected AusenciaProgramada() {
    }

    public AusenciaProgramada(UUID tenantId, UUID vinculoId, TipoAusencia tipo,
                              LocalDate dataInicio, LocalDate dataFim, String observacao) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.tipo = tipo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.observacao = observacao;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public int dias() {
        return (int) (dataFim.toEpochDay() - dataInicio.toEpochDay()) + 1;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVinculoId() {
        return vinculoId;
    }

    public TipoAusencia getTipo() {
        return tipo;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public String getObservacao() {
        return observacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
