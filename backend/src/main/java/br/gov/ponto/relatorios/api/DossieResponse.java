package br.gov.ponto.relatorios.api;

import java.time.LocalDate;
import java.util.List;

/**
 * Dossiê de conformidade / escudo jurídico empacotado (12.1.7 / 12.5.6 / 12.3.4): reúne, em
 * uma resposta, as evidências de defesa do ente perante o TCM-GO — IN 008/2021, AFD (Portaria
 * 671) com hash, integridade da cadeia (tamper-evidence), indicadores e o prazo de submissão.
 */
public record DossieResponse(
        String competencia,
        ConformidadeResponse conformidade,
        String afdHashSha256,
        int afdTotalRegistros,
        boolean cadeiaIntegra,
        String integridadeDetalhe,
        IndicadoresResponse indicadores,
        LocalDate prazoSubmissao,
        long diasRestantes,
        List<String> escudos
) {
}
