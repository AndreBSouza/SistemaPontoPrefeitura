package br.gov.ponto.relatorios.api;

public record AfdResponse(
        String competencia,
        int totalRegistros,
        String hashSha256,
        String conteudo,
        String assinatura
) {
}
