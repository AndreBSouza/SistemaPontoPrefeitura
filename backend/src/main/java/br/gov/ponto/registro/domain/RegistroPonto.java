package br.gov.ponto.registro.domain;

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
import java.util.UUID;

/**
 * Marcacao de ponto. NSR sequencial e imutavel por tenant; idempotencyKey unica
 * por tenant garante deduplicacao na sincronizacao offline.
 */
@Entity
@Table(name = "registro_ponto")
public class RegistroPonto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "vinculo_id", nullable = false)
    private UUID vinculoId;

    @Column(nullable = false)
    private long nsr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMarcacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemRegistro origem;

    @Column(name = "data_hora_servidor", nullable = false)
    private Instant dataHoraServidor;

    @Column(name = "data_hora_dispositivo")
    private Instant dataHoraDispositivo;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(nullable = false)
    private boolean offline;

    @Column(name = "fora_da_cerca", nullable = false)
    private boolean foraDaCerca;

    @Column(length = 64)
    private String hash;

    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected RegistroPonto() {
    }

    public RegistroPonto(UUID tenantId, UUID vinculoId, long nsr, TipoMarcacao tipo, OrigemRegistro origem,
                         Instant dataHoraServidor, Instant dataHoraDispositivo,
                         BigDecimal latitude, BigDecimal longitude, boolean offline, String idempotencyKey) {
        this(tenantId, vinculoId, nsr, tipo, origem, dataHoraServidor, dataHoraDispositivo,
                latitude, longitude, offline, idempotencyKey, false);
    }

    public RegistroPonto(UUID tenantId, UUID vinculoId, long nsr, TipoMarcacao tipo, OrigemRegistro origem,
                         Instant dataHoraServidor, Instant dataHoraDispositivo,
                         BigDecimal latitude, BigDecimal longitude, boolean offline, String idempotencyKey,
                         boolean foraDaCerca) {
        this.tenantId = tenantId;
        this.vinculoId = vinculoId;
        this.nsr = nsr;
        this.tipo = tipo;
        this.origem = origem;
        this.dataHoraServidor = dataHoraServidor;
        this.dataHoraDispositivo = dataHoraDispositivo;
        this.latitude = latitude;
        this.longitude = longitude;
        this.offline = offline;
        this.idempotencyKey = idempotencyKey;
        this.foraDaCerca = foraDaCerca;
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

    public long getNsr() {
        return nsr;
    }

    public TipoMarcacao getTipo() {
        return tipo;
    }

    public OrigemRegistro getOrigem() {
        return origem;
    }

    public Instant getDataHoraServidor() {
        return dataHoraServidor;
    }

    public Instant getDataHoraDispositivo() {
        return dataHoraDispositivo;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public boolean isOffline() {
        return offline;
    }

    public boolean isForaDaCerca() {
        return foraDaCerca;
    }

    public String getHash() {
        return hash;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    /** Define o elo da cadeia de integridade (no momento da gravacao). */
    public void definirCadeia(String hashAnterior, String hash) {
        this.hashAnterior = hashAnterior;
        this.hash = hash;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
