package br.gov.ponto.cadastro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Regras de ponto proprias de um Orgao/Unidade (lotacao). Todos os campos sao
 * opcionais (null = nao definido no orgao); a resolucao das regras efetivas do
 * vinculo cuida da heranca e dos defaults. Value object imutavel (DDD).
 */
@Embeddable
public class RegrasPonto {

    @Column(name = "jornada_padrao_id")
    private UUID jornadaPadraoId;

    @Column(name = "tolerancia_minutos")
    private Integer toleranciaMinutos;

    @Column(name = "banco_horas_habilitado")
    private Boolean bancoHorasHabilitado;

    @Column(name = "geofence_latitude")
    private BigDecimal geofenceLatitude;

    @Column(name = "geofence_longitude")
    private BigDecimal geofenceLongitude;

    @Column(name = "geofence_raio_metros")
    private Integer geofenceRaioMetros;

    @Column(name = "teto_banco_horas_minutos")
    private Integer tetoBancoHorasMinutos;

    @Column(name = "verificacao_obrigatoria")
    private Boolean verificacaoObrigatoria;

    /** Modo adaptação: até esta data o órgão só registra, sem descontar/penalizar (anti-revolta). */
    @Column(name = "adaptacao_ate")
    private LocalDate adaptacaoAte;

    /** Teletrabalho/home office: a geofence do órgão não se aplica (bate de qualquer lugar). */
    @Column(name = "teletrabalho")
    private Boolean teletrabalho;

    protected RegrasPonto() {
    }

    public RegrasPonto(UUID jornadaPadraoId, Integer toleranciaMinutos, Boolean bancoHorasHabilitado,
                       BigDecimal geofenceLatitude, BigDecimal geofenceLongitude, Integer geofenceRaioMetros) {
        this(jornadaPadraoId, toleranciaMinutos, bancoHorasHabilitado,
                geofenceLatitude, geofenceLongitude, geofenceRaioMetros, null);
    }

    public RegrasPonto(UUID jornadaPadraoId, Integer toleranciaMinutos, Boolean bancoHorasHabilitado,
                       BigDecimal geofenceLatitude, BigDecimal geofenceLongitude, Integer geofenceRaioMetros,
                       Integer tetoBancoHorasMinutos) {
        this(jornadaPadraoId, toleranciaMinutos, bancoHorasHabilitado, geofenceLatitude, geofenceLongitude,
                geofenceRaioMetros, tetoBancoHorasMinutos, null);
    }

    public RegrasPonto(UUID jornadaPadraoId, Integer toleranciaMinutos, Boolean bancoHorasHabilitado,
                       BigDecimal geofenceLatitude, BigDecimal geofenceLongitude, Integer geofenceRaioMetros,
                       Integer tetoBancoHorasMinutos, Boolean verificacaoObrigatoria) {
        this(jornadaPadraoId, toleranciaMinutos, bancoHorasHabilitado, geofenceLatitude, geofenceLongitude,
                geofenceRaioMetros, tetoBancoHorasMinutos, verificacaoObrigatoria, null);
    }

    public RegrasPonto(UUID jornadaPadraoId, Integer toleranciaMinutos, Boolean bancoHorasHabilitado,
                       BigDecimal geofenceLatitude, BigDecimal geofenceLongitude, Integer geofenceRaioMetros,
                       Integer tetoBancoHorasMinutos, Boolean verificacaoObrigatoria,
                       LocalDate adaptacaoAte) {
        this(jornadaPadraoId, toleranciaMinutos, bancoHorasHabilitado, geofenceLatitude, geofenceLongitude,
                geofenceRaioMetros, tetoBancoHorasMinutos, verificacaoObrigatoria, adaptacaoAte, null);
    }

    public RegrasPonto(UUID jornadaPadraoId, Integer toleranciaMinutos, Boolean bancoHorasHabilitado,
                       BigDecimal geofenceLatitude, BigDecimal geofenceLongitude, Integer geofenceRaioMetros,
                       Integer tetoBancoHorasMinutos, Boolean verificacaoObrigatoria,
                       LocalDate adaptacaoAte, Boolean teletrabalho) {
        this.jornadaPadraoId = jornadaPadraoId;
        this.toleranciaMinutos = toleranciaMinutos;
        this.bancoHorasHabilitado = bancoHorasHabilitado;
        this.geofenceLatitude = geofenceLatitude;
        this.geofenceLongitude = geofenceLongitude;
        this.geofenceRaioMetros = geofenceRaioMetros;
        this.tetoBancoHorasMinutos = tetoBancoHorasMinutos;
        this.verificacaoObrigatoria = verificacaoObrigatoria;
        this.adaptacaoAte = adaptacaoAte;
        this.teletrabalho = teletrabalho;
    }

    public static RegrasPonto vazia() {
        return new RegrasPonto();
    }

    public boolean temGeofence() {
        return geofenceLatitude != null && geofenceLongitude != null && geofenceRaioMetros != null;
    }

    /** Banco de horas habilitado no orgao; default do sistema = habilitado. */
    public boolean bancoHorasHabilitado() {
        return bancoHorasHabilitado == null || bancoHorasHabilitado;
    }

    public UUID getJornadaPadraoId() {
        return jornadaPadraoId;
    }

    public Integer getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public Boolean getBancoHorasHabilitado() {
        return bancoHorasHabilitado;
    }

    public BigDecimal getGeofenceLatitude() {
        return geofenceLatitude;
    }

    public BigDecimal getGeofenceLongitude() {
        return geofenceLongitude;
    }

    public Integer getGeofenceRaioMetros() {
        return geofenceRaioMetros;
    }

    public Integer getTetoBancoHorasMinutos() {
        return tetoBancoHorasMinutos;
    }

    /** Teto de acumulo do banco de horas do orgao; usa o default do sistema se nao definido. */
    public int tetoBancoHorasMinutos(int defaultMinutos) {
        return tetoBancoHorasMinutos != null && tetoBancoHorasMinutos > 0 ? tetoBancoHorasMinutos : defaultMinutos;
    }

    public Boolean getVerificacaoObrigatoria() {
        return verificacaoObrigatoria;
    }

    /** Exige verificação (biometria/PIN/desenho do aparelho, ou facial) antes de bater; default = não exige. */
    public boolean verificacaoObrigatoria() {
        return verificacaoObrigatoria != null && verificacaoObrigatoria;
    }

    public LocalDate getAdaptacaoAte() {
        return adaptacaoAte;
    }

    /**
     * Modo adaptação ativo na data: até {@code adaptacaoAte} (inclusive) o órgão só registra,
     * sem aplicar descontos/penalidades (atraso, falta, saída antecipada). Default = sem adaptação.
     */
    public boolean emAdaptacao(LocalDate data) {
        return adaptacaoAte != null && !data.isAfter(adaptacaoAte);
    }

    public Boolean getTeletrabalho() {
        return teletrabalho;
    }

    /** Teletrabalho/home office: a geofence não se aplica; default = não. */
    public boolean teletrabalho() {
        return teletrabalho != null && teletrabalho;
    }
}
