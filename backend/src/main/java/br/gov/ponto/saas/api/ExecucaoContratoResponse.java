package br.gov.ponto.saas.api;

import java.math.BigDecimal;

/**
 * Relatório de execução mensal do contrato — para anexar ao processo de pagamento/liquidação:
 * "no mês X, o sistema atendeu N servidores; parcela contratual R$ Y".
 */
public record ExecucaoContratoResponse(
        String competencia,
        boolean contratoVigente,
        BigDecimal valorMensal,
        int servidoresAtivos,
        long dispositivosAtivos,
        long registrosNoPeriodo,
        String numeroProcesso,
        String empenho
) {
}
