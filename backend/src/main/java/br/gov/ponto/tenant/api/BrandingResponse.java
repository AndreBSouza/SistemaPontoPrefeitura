package br.gov.ponto.tenant.api;

/** Dados do ente para o painel: identidade visual (com defaults) + CNPJ (usado no AFD). */
public record BrandingResponse(
        String nomeEnte,
        String nomeApp,
        String logoUrl,
        String corPrimaria,
        String corAcento,
        String cnpj
) {
}
