package br.gov.ponto.bancohoras.domain;

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

/** Lancamento no banco de horas (minutos positivo = credito, negativo = debito). */
@Entity
@Table(name = "banco_horas_lancamento")
public class BancoHorasLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private int minutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoLancamento tipo;

    @Column(length = 300)
    private String descricao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected BancoHorasLancamento() {
    }

    public BancoHorasLancamento(UUID tenantId, UUID vinculoId, LocalDate data,
                                int minutos, TipoLancamento tipo, String descricao) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.data = data;
        this.minutos = minutos;
        this.tipo = tipo;
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

    public LocalDate getData() {
        return data;
    }

    public int getMinutos() {
        return minutos;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }
}
