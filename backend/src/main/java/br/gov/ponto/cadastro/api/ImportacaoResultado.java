package br.gov.ponto.cadastro.api;

import java.util.List;

public record ImportacaoResultado(
        int totalLinhas,
        int importados,
        List<ErroLinha> erros
) {
    public record ErroLinha(int linha, String mensagem) {
    }
}
