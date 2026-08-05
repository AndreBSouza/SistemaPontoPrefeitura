package br.gov.ponto.relatorios.api;

/** Indicadores de gestão/adesão do ente (painel do gestor + piloto gradual). */
public record IndicadoresResponse(
        int totalVinculos,
        long dispositivosAtivos,
        int adesaoPercentual,
        long registrosNoPeriodo
) {
}
