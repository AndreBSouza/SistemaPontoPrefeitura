package br.gov.ponto.espelho.api;

import java.util.List;
import java.util.UUID;

/**
 * Painel "o que falta fechar" (12.6.2): vínculos ativos cuja competência ainda não
 * foi fechada, agrupados por órgão, com totais para acompanhamento do RH.
 */
public record PendenciaFechamentoResponse(
        String competencia,
        int totalVinculos,
        int fechadas,
        int pendentes,
        List<OrgaoPendencia> orgaos
) {
    /** Pendências de um órgão (lotacaoId nulo = vínculos sem órgão). */
    public record OrgaoPendencia(UUID lotacaoId, String orgao, int pendentes, List<Item> itens) {
    }

    /** Vínculo com competência ainda aberta. */
    public record Item(UUID vinculoId, String matricula, UUID servidorId, String servidor) {
    }
}
