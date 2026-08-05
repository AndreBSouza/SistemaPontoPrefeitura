package br.gov.ponto.relatorios.api;

import java.math.BigDecimal;

/**
 * Painel de ROI / economia (12.3.1): estima, em R$, a economia do controle de ponto —
 * hora extra controlada e queda de absenteísmo. Combina o apurado (BI) com percentuais de
 * redução informados pelo gestor (transparente, sem número inventado).
 */
public record RoiResponse(
        String competencia,
        BigDecimal custoHoraExtraAtualReais,
        BigDecimal economiaHoraExtraReais,
        BigDecimal custoAbsenteismoAtualReais,
        BigDecimal economiaAbsenteismoReais,
        BigDecimal economiaTotalEstimadaReais
) {
}
