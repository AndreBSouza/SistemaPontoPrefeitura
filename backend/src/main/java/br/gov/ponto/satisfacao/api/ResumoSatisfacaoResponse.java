package br.gov.ponto.satisfacao.api;

import java.util.List;
import java.util.Map;

/**
 * Resumo da pesquisa de satisfação (12.1.3): total de respostas, média e distribuição por
 * nota (1..5) — métrica de aceitação do ponto eletrônico para o prefeito/RH.
 */
public record ResumoSatisfacaoResponse(
        int total,
        double media,
        Map<Integer, Integer> distribuicao,
        List<String> comentarios
) {
}
