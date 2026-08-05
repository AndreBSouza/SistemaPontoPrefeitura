package br.gov.ponto.integracao.api;

import java.util.List;
import java.util.UUID;

/**
 * Conferência cruzada folha × frequência (12.6.12): compara o que a folha pagou/descontou
 * (entrada externa) com o que a frequência apurou, sinalizando divergências.
 */
public record ConferenciaResponse(
        String competencia,
        int totalDivergencias,
        List<Item> itens
) {
    public record Item(
            UUID vinculoId,
            int horaExtraApuradaMinutos,
            int horaExtraPagaMinutos,
            int faltasApuradas,
            int faltasDescontadas,
            boolean divergente
    ) {
    }
}
