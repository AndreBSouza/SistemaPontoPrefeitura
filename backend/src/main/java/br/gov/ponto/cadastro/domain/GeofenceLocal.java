package br.gov.ponto.cadastro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Área de referência adicional de um órgão (local volante), para a verificação de localização
 * (multi-geofence, 12.3.10). Um órgão pode ter várias — ex.: secretaria com vários postos. A
 * batida é considerada "fora da área" só quando está fora de TODAS as áreas do órgão. Como toda
 * a geofence no sistema, é apenas verificação do administrador: nunca bloqueia nem alerta o servidor.
 */
@Entity
@Table(name = "geofence_local")
public class GeofenceLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "lotacao_id", nullable = false)
    private UUID lotacaoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private BigDecimal latitude;

    @Column(nullable = false)
    private BigDecimal longitude;

    @Column(name = "raio_metros", nullable = false)
    private Integer raioMetros;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected GeofenceLocal() {
    }

    public GeofenceLocal(UUID tenantId, UUID lotacaoId, String nome,
                         BigDecimal latitude, BigDecimal longitude, Integer raioMetros) {
        this.tenantId = tenantId;
        this.lotacaoId = lotacaoId;
        this.nome = nome;
        this.latitude = latitude;
        this.longitude = longitude;
        this.raioMetros = raioMetros;
    }

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    /** Converte para a regra de domínio pura (cerca circular) usada na verificação da batida. */
    public Geofence paraDominio() {
        return new Geofence(latitude.doubleValue(), longitude.doubleValue(), raioMetros);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getLotacaoId() {
        return lotacaoId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public Integer getRaioMetros() {
        return raioMetros;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
