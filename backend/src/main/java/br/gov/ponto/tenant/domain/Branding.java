package br.gov.ponto.tenant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Identidade visual (white-label) do ente: nome exibido no app, logo e cores.
 * Todos os campos são opcionais; os acessores aplicam os defaults do sistema.
 */
@Embeddable
public class Branding {

    @Column(name = "nome_app", length = 60)
    private String nomeApp;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cor_primaria", length = 9)
    private String corPrimaria;

    @Column(name = "cor_acento", length = 9)
    private String corAcento;

    protected Branding() {
    }

    public Branding(String nomeApp, String logoUrl, String corPrimaria, String corAcento) {
        this.nomeApp = nomeApp;
        this.logoUrl = logoUrl;
        this.corPrimaria = corPrimaria;
        this.corAcento = corAcento;
    }

    public static Branding vazia() {
        return new Branding();
    }

    /** Cópia desta identidade trocando apenas a URL do logo (preserva nome e cores brutos). */
    public Branding comLogoUrl(String novaLogoUrl) {
        return new Branding(nomeApp, novaLogoUrl, corPrimaria, corAcento);
    }

    public String nomeAppOu(String padrao) {
        return nomeApp != null && !nomeApp.isBlank() ? nomeApp : padrao;
    }

    /** Cor primária do ente; default = azul gov.br. */
    public String corPrimaria() {
        return corPrimaria != null && !corPrimaria.isBlank() ? corPrimaria : "#1351B4";
    }

    /** Cor de acento do ente; default = verde-registro. */
    public String corAcento() {
        return corAcento != null && !corAcento.isBlank() ? corAcento : "#1F6E5C";
    }

    public String getLogoUrl() {
        return logoUrl;
    }
}
