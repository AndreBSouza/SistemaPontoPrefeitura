package br.gov.ponto.cadastro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Lotacao (secretaria/unidade) do organograma do ente. */
@Entity
@Table(name = "lotacao")
public class Lotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String nome;

    private String sigla;

    @Column(name = "chefia_servidor_id")
    private UUID chefiaServidorId;

    /** Regras de ponto proprias do orgao (jornada padrao, tolerancia, banco de horas, geofence). */
    @Embedded
    private RegrasPonto regras = RegrasPonto.vazia();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Lotacao() {
    }

    public Lotacao(UUID tenantId, String nome, String sigla) {
        this.tenantId = tenantId;
        this.nome = nome;
        this.sigla = sigla;
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

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public UUID getChefiaServidorId() {
        return chefiaServidorId;
    }

    public void definirChefia(UUID chefiaServidorId) {
        this.chefiaServidorId = chefiaServidorId;
    }

    public RegrasPonto getRegras() {
        // Hibernate carrega o embeddable como null quando todas as colunas sao null.
        return regras == null ? RegrasPonto.vazia() : regras;
    }

    public void definirRegras(RegrasPonto regras) {
        this.regras = regras == null ? RegrasPonto.vazia() : regras;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
