package br.gov.ponto.relatorios.api;

import java.math.BigDecimal;

/**
 * Previsão de absenteísmo + projeção de custo de hora extra (12.4.12), heurística sobre os
 * {@code mesesBase} meses anteriores à competência-alvo.
 */
public record PrevisaoResponse(
        String competencia,
        int mesesBase,
        long faltasProjetadas,
        int horaExtraProjetadaMin,
        BigDecimal custoHoraExtraProjetado,
        String tendenciaFaltas,
        String tendenciaHoraExtra
) {
}
