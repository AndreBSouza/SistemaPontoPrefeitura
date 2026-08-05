package br.gov.ponto.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

/** Ente municipal (tenant): prefeitura, camara, autarquia, fundo. */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_poder", nullable = false)
    private TipoPoder tipoPoder;

    /** CNPJ do ente (14 dígitos), usado no cabeçalho do AFD. Opcional até ser cadastrado. */
    @Column(length = 14)
    private String cnpj;

    /** Subdomínio do ente (ex.: "cidade" em cidade.ponto.gov.br); o app/login se autoconfigura por ele. */
    @Column(length = 60, unique = true)
    private String subdominio;

    /** Identidade visual (white-label) do ente: nome do app, logo e cores. */
    @Embedded
    private Branding branding;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Tenant() {
    }

    public Tenant(String nome, String slug, TipoPoder tipoPoder) {
        this.nome = nome;
        this.slug = slug;
        this.tipoPoder = tipoPoder;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSubdominio() {
        return subdominio;
    }

    public void setSubdominio(String subdominio) {
        this.subdominio = subdominio;
    }

    public TipoPoder getTipoPoder() {
        return tipoPoder;
    }

    public void setTipoPoder(TipoPoder tipoPoder) {
        this.tipoPoder = tipoPoder;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Branding getBranding() {
        return branding;
    }

    public void setBranding(Branding branding) {
        this.branding = branding;
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
