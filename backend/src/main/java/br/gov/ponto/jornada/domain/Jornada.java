package br.gov.ponto.jornada.domain;

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
import java.util.UUID;

/** Jornada de trabalho (modelo de carga/tolerancia/intervalo). */
@Entity
@Table(name = "jornada")
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoJornada tipo;

    @Column(name = "carga_horaria_semanal_min", nullable = false)
    private int cargaHorariaSemanalMin;

    @Column(name = "tolerancia_min", nullable = false)
    private int toleranciaMin;

    @Column(name = "intervalo_min", nullable = false)
    private int intervaloMin;

    /** Minutos semanais de hora-atividade (magistério/Lei do Piso); nulo = não se aplica. */
    @Column(name = "hora_atividade_min")
    private Integer horaAtividadeMin;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Jornada() {
    }

    public Jornada(UUID tenantId, String nome, TipoJornada tipo,
                   int cargaHorariaSemanalMin, int toleranciaMin, int intervaloMin) {
        this.tenantId = tenantId;
        this.nome = nome;
        this.tipo = tipo;
        this.cargaHorariaSemanalMin = cargaHorariaSemanalMin;
        this.toleranciaMin = toleranciaMin;
        this.intervaloMin = intervaloMin;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public String getNome() {
        return nome;
    }

    public TipoJornada getTipo() {
        return tipo;
    }

    public int getCargaHorariaSemanalMin() {
        return cargaHorariaSemanalMin;
    }

    public int getToleranciaMin() {
        return toleranciaMin;
    }

    public Integer getHoraAtividadeMin() {
        return horaAtividadeMin;
    }

    public void setHoraAtividadeMin(Integer horaAtividadeMin) {
        this.horaAtividadeMin = horaAtividadeMin;
    }

    public int getIntervaloMin() {
        return intervaloMin;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
