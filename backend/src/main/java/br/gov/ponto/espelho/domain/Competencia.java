package br.gov.ponto.espelho.domain;

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

/** Competencia mensal de um vinculo: fechamento e ciencia/assinatura. */
@Entity
@Table(name = "competencia")
public class Competencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(name = "ano_mes", nullable = false)
    private LocalDate anoMes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCompetencia status = StatusCompetencia.ABERTA;

    @Column(name = "fechado_em")
    private Instant fechadoEm;

    @Column(name = "reaberto_em")
    private Instant reabertoEm;

    @Column(name = "motivo_reabertura", length = 500)
    private String motivoReabertura;

    @Column(name = "ciencia_em")
    private Instant cienciaEm;

    @Column(name = "ciencia_evidencia", length = 200)
    private String cienciaEvidencia;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Competencia() {
    }

    public Competencia(UUID tenantId, UUID vinculoId, LocalDate anoMes) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.anoMes = anoMes;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public void fechar() {
        this.status = StatusCompetencia.FECHADA;
        this.fechadoEm = Instant.now();
    }

    public void reabrir(String motivo) {
        this.status = StatusCompetencia.ABERTA;
        this.reabertoEm = Instant.now();
        this.motivoReabertura = motivo;
    }

    public void darCiencia(String evidencia) {
        this.cienciaEm = Instant.now();
        this.cienciaEvidencia = evidencia;
    }

    public boolean isFechada() {
        return status == StatusCompetencia.FECHADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVinculoId() {
        return vinculoId;
    }

    public LocalDate getAnoMes() {
        return anoMes;
    }

    public StatusCompetencia getStatus() {
        return status;
    }

    public Instant getFechadoEm() {
        return fechadoEm;
    }

    public Instant getReabertoEm() {
        return reabertoEm;
    }

    public String getMotivoReabertura() {
        return motivoReabertura;
    }

    public Instant getCienciaEm() {
        return cienciaEm;
    }

    public String getCienciaEvidencia() {
        return cienciaEvidencia;
    }
}
