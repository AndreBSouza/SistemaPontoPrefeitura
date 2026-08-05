package br.gov.ponto.saas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Contrato de fornecimento do sistema ao ente. No setor público a contratação é por
 * dispensa/licitação com <b>valor fixo</b> (global e/ou parcela mensal), empenhado no orçamento —
 * não há cobrança por demanda.
 */
@Entity
@Table(name = "contrato")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModalidadeContratacao modalidade;

    @Column(name = "numero_processo", length = 60)
    private String numeroProcesso;

    @Column(length = 60)
    private String empenho;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim", nullable = false)
    private LocalDate vigenciaFim;

    @Column(name = "valor_global", precision = 15, scale = 2)
    private BigDecimal valorGlobal;

    @Column(name = "valor_mensal", precision = 15, scale = 2)
    private BigDecimal valorMensal;

    @Column(length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Contrato() {
    }

    public Contrato(UUID tenantId, ModalidadeContratacao modalidade, String numeroProcesso, String empenho,
                    LocalDate vigenciaInicio, LocalDate vigenciaFim, BigDecimal valorGlobal,
                    BigDecimal valorMensal, String observacao) {
        this.tenantId = tenantId;
        this.modalidade = modalidade;
        this.numeroProcesso = numeroProcesso;
        this.empenho = empenho;
        this.vigenciaInicio = vigenciaInicio;
        this.vigenciaFim = vigenciaFim;
        this.valorGlobal = valorGlobal;
        this.valorMensal = valorMensal;
        this.observacao = observacao;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    /** O contrato está vigente na data informada? */
    public boolean vigenteEm(LocalDate data) {
        return !data.isBefore(vigenciaInicio) && !data.isAfter(vigenciaFim);
    }

    public UUID getId() {
        return id;
    }

    public ModalidadeContratacao getModalidade() {
        return modalidade;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public String getEmpenho() {
        return empenho;
    }

    public LocalDate getVigenciaInicio() {
        return vigenciaInicio;
    }

    public LocalDate getVigenciaFim() {
        return vigenciaFim;
    }

    public BigDecimal getValorGlobal() {
        return valorGlobal;
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public String getObservacao() {
        return observacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
