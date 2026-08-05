package br.gov.ponto.relatorios.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra (POJO testável) do limite de gasto com pessoal da LRF (Lei de Responsabilidade
 * Fiscal): para o Executivo municipal, o limite é 54% da RCL (Receita Corrente Líquida)
 * e o limite prudencial é 95% desse teto (≈ 51,3% da RCL). Projeta o percentual após um
 * acréscimo de gasto (ex.: autorizar horas extras) e classifica o risco.
 */
public final class LimiteLrf {

    /** Limite legal de gasto com pessoal do Executivo municipal (54% da RCL). */
    public static final BigDecimal LIMITE_EXECUTIVO = new BigDecimal("0.54");
    /** Fator do limite prudencial (95% do limite legal). */
    public static final BigDecimal FATOR_PRUDENCIAL = new BigDecimal("0.95");

    private LimiteLrf() {
    }

    public enum Risco { OK, ALERTA, PRUDENCIAL, ESTOURO }

    /**
     * Percentual do gasto com pessoal (já incluído o acréscimo) sobre a RCL, em pontos
     * percentuais (ex.: 53.2). RCL <= 0 → 0.
     */
    public static BigDecimal percentualSobreRcl(BigDecimal gastoPessoal, BigDecimal acrescimo, BigDecimal rcl) {
        if (rcl == null || rcl.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = nz(gastoPessoal).add(nz(acrescimo));
        return total.divide(rcl, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    /** Classifica o risco do percentual resultante frente aos limites da LRF. */
    public static Risco classificar(BigDecimal percentual) {
        BigDecimal limite = LIMITE_EXECUTIVO.multiply(new BigDecimal("100"));            // 54.00
        BigDecimal prudencial = limite.multiply(FATOR_PRUDENCIAL).setScale(2, RoundingMode.HALF_UP); // 51.30
        if (percentual.compareTo(limite) > 0) {
            return Risco.ESTOURO;
        }
        if (percentual.compareTo(prudencial) >= 0) {
            return Risco.PRUDENCIAL;
        }
        // Zona de alerta: a 2 p.p. do limite prudencial.
        if (percentual.compareTo(prudencial.subtract(new BigDecimal("2.00"))) >= 0) {
            return Risco.ALERTA;
        }
        return Risco.OK;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
