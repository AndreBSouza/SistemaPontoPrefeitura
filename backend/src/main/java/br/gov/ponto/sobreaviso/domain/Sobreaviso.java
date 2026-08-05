package br.gov.ponto.sobreaviso.domain;

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
 * Período de sobreaviso (on-call) de um vínculo: o servidor fica à disposição fora do expediente.
 * Conta de forma diferente do trabalho efetivo (tipicamente pago a 1/3 da hora) — por isso é um
 * registro à parte, somado por competência e exportado para a folha. Não interfere na apuração
 * normal do dia (o expediente regular, se houver, é apurado como sempre).
 */
@Entity
@Table(name = "sobreaviso")
public class Sobreaviso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(nullable = false)
    private LocalDate data;

    /** Minutos de sobreaviso no dia. */
    @Column(nullable = false)
    private int minutos;

    @Column(length = 200)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Sobreaviso() {
    }

    public Sobreaviso(UUID tenantId, UUID vinculoId, LocalDate data, int minutos, String observacao) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.data = data;
        this.minutos = minutos;
        this.observacao = observacao;
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

    public UUID getVinculoId() {
        return vinculoId;
    }

    public LocalDate getData() {
        return data;
    }

    public int getMinutos() {
        return minutos;
    }

    public String getObservacao() {
        return observacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
