package br.gov.ponto.publico.api;

/**
 * Transparência ativa / dados abertos (12.3.6): frequência AGREGADA do ente (sem qualquer
 * dado pessoal) para publicação no portal de transparência.
 */
public record TransparenciaResponse(
        String ente,
        String competencia,
        int totalServidores,
        int totalVinculos,
        long totalRegistros
) {
}
