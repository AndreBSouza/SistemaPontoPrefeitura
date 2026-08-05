package br.gov.ponto.relatorios.domain;

import java.util.List;
import java.util.Map;

/**
 * Heurística explicável (sem IA) para achar valores atípicos num conjunto: uma chave é
 * "outlier" quando seu valor supera a média × fator E um piso mínimo (evita falso positivo
 * em amostras pequenas). Ex.: hora extra muito acima da média do ente.
 */
public final class DetectorAnomalia {

    private DetectorAnomalia() {
    }

    public static <K> List<K> outliers(Map<K, Integer> valores, double fator, int piso) {
        double media = valores.values().stream().filter(v -> v > 0).mapToInt(Integer::intValue).average().orElse(0);
        if (media <= 0) {
            return List.of();
        }
        double limiar = Math.max(media * fator, piso);
        return valores.entrySet().stream()
                .filter(e -> e.getValue() > limiar)
                .map(Map.Entry::getKey)
                .toList();
    }
}
