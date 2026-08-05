package br.gov.ponto.apuracao.domain;

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

/** Abono/justificativa de frequencia, com workflow de aprovacao. */
@Entity
@Table(name = "justificativa")
public class Justificativa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoJustificativa tipo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(length = 500)
    private String motivo;

    @Column(length = 300)
    private String anexo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusJustificativa status = StatusJustificativa.PENDENTE;

    @Column(name = "aprovador_id")
    private UUID aprovadorId;

    @Column(name = "decisao_em")
    private Instant decisaoEm;

    @Column(name = "motivo_decisao", length = 500)
    private String motivoDecisao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Justificativa() {
    }

    public Justificativa(UUID tenantId, UUID vinculoId, TipoJustificativa tipo,
                         LocalDate dataInicio, LocalDate dataFim, String motivo, String anexo) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.tipo = tipo;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.motivo = motivo;
        this.anexo = anexo;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public void decidir(StatusJustificativa novoStatus, UUID aprovadorId, String motivoDecisao) {
        this.status = novoStatus;
        this.aprovadorId = aprovadorId;
        this.motivoDecisao = motivoDecisao;
        this.decisaoEm = Instant.now();
    }

    public boolean cobre(LocalDate data) {
        return !data.isBefore(dataInicio) && !data.isAfter(dataFim);
    }

    public UUID getId() {
        return id;
    }

    public UUID getVinculoId() {
        return vinculoId;
    }

    public TipoJustificativa getTipo() {
        return tipo;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getAnexo() {
        return anexo;
    }

    public StatusJustificativa getStatus() {
        return status;
    }

    public UUID getAprovadorId() {
        return aprovadorId;
    }

    public Instant getDecisaoEm() {
        return decisaoEm;
    }

    public String getMotivoDecisao() {
        return motivoDecisao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
