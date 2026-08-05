package br.gov.ponto.relatorios.api;

public record ConformidadeResponse(
        String competencia,
        long totalServidores,
        long totalVinculos,
        long totalRegistros,
        String descricao
) {
}
