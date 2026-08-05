package br.gov.ponto.cadastro.domain;

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

/** Vinculo/matricula do servidor. Matricula unica por tenant. */
@Entity
@Table(name = "vinculo")
public class Vinculo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "servidor_id", nullable = false)
    private UUID servidorId;

    @Column(nullable = false, length = 30)
    private String matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Regime regime;

    private String cargo;

    @Column(name = "carga_horaria_semanal")
    private Integer cargaHorariaSemanal;

    @Column(name = "lotacao_id")
    private UUID lotacaoId;

    @Column(name = "data_admissao")
    private LocalDate dataAdmissao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Vinculo() {
    }

    public Vinculo(UUID tenantId, UUID servidorId, String matricula, Regime regime) {
        this.tenantId = tenantId;
        this.servidorId = servidorId;
        this.matricula = matricula;
        this.regime = regime;
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

    public UUID getServidorId() {
        return servidorId;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public Regime getRegime() {
        return regime;
    }

    public void setRegime(Regime regime) {
        this.regime = regime;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public Integer getCargaHorariaSemanal() {
        return cargaHorariaSemanal;
    }

    public void setCargaHorariaSemanal(Integer cargaHorariaSemanal) {
        this.cargaHorariaSemanal = cargaHorariaSemanal;
    }

    public UUID getLotacaoId() {
        return lotacaoId;
    }

    public void setLotacaoId(UUID lotacaoId) {
        this.lotacaoId = lotacaoId;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(LocalDate dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
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
