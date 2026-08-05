package br.gov.ponto.jornada.domain;

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

/** Atribuicao de uma jornada a um vinculo, por periodo de vigencia. */
@Entity
@Table(name = "escala")
public class Escala {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(name = "jornada_id", nullable = false)
    private UUID jornadaId;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Escala() {
    }

    public Escala(UUID tenantId, UUID vinculoId, UUID jornadaId, LocalDate dataInicio, LocalDate dataFim) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.jornadaId = jornadaId;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    /** Troca a jornada atribuida (usado em troca de turno). */
    public void definirJornada(UUID novaJornadaId) {
        this.jornadaId = novaJornadaId;
    }

    /** Vigente na data quando inicio <= data <= fim (fim em aberto = infinito). */
    public boolean vigenteEm(LocalDate data) {
        return !data.isBefore(dataInicio) && (dataFim == null || !data.isAfter(dataFim));
    }

    /** Vigencia em aberto (sem data fim) e tratada como infinita. */
    public boolean sobrepoeCom(LocalDate outroInicio, LocalDate outroFim) {
        LocalDate fimA = this.dataFim == null ? LocalDate.MAX : this.dataFim;
        LocalDate fimB = outroFim == null ? LocalDate.MAX : outroFim;
        return !this.dataInicio.isAfter(fimB) && !outroInicio.isAfter(fimA);
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

    public UUID getJornadaId() {
        return jornadaId;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
