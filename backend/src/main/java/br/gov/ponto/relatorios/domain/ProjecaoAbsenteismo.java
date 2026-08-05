package br.gov.ponto.relatorios.domain;

import java.util.List;

/**
 * Projeção heurística (sem IA) de uma série mensal: base = média dos meses + tendência média por
 * mês; sempre não-negativa. Explicável — o gestor entende "média dos últimos meses, ajustada pela
 * tendência". Ex.: projetar faltas e minutos de hora extra do próximo mês.
 */
public final class ProjecaoAbsenteismo {

    private ProjecaoAbsenteismo() {
    }

    /** Próximo valor projetado: média da série + tendência por mês (mínimo 0). */
    public static double projetar(List<Integer> serie) {
        if (serie.isEmpty()) {
            return 0;
        }
        double media = serie.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.max(0, media + tendenciaPorMes(serie));
    }

    /** Variação média por mês: (último - primeiro) / (n-1). Positivo = subindo. */
    public static double tendenciaPorMes(List<Integer> serie) {
        if (serie.size() < 2) {
            return 0;
        }
        return (serie.get(serie.size() - 1) - serie.get(0)) / (double) (serie.size() - 1);
    }

    /** Rótulo qualitativo da tendência ("subindo"/"estável"/"caindo") dado um limiar de indiferença. */
    public static String rotuloTendencia(List<Integer> serie, double limiar) {
        double t = tendenciaPorMes(serie);
        if (t > limiar) {
            return "subindo";
        }
        if (t < -limiar) {
            return "caindo";
        }
        return "estável";
    }
}
