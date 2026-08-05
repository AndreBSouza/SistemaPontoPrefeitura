package br.gov.ponto.relatorios.api;

import java.math.BigDecimal;

/**
 * Resultado do simulador orçamentário (12.5.5) com alerta de limite da LRF (12.5.1):
 * "autorizar X horas extras custa Y e me deixa a Z% da RCL".
 */
public record SimulacaoResponse(
        BigDecimal custoHoraExtraReais,
        BigDecimal gastoProjetadoReais,
        BigDecimal percentualRclProjetado,
        String risco,
        BigDecimal limitePercentual,
        BigDecimal prudencialPercentual
) {
}
