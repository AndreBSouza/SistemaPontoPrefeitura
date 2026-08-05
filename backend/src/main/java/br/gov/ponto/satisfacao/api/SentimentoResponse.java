package br.gov.ponto.satisfacao.api;

/** Análise de sentimento da pesquisa. {@code disponivel=false} = recurso desligado ou sem provedor. */
public record SentimentoResponse(boolean disponivel, int total, int positivos, int neutros,
                                 int negativos, String resumo) {

    public static SentimentoResponse indisponivel() {
        return new SentimentoResponse(false, 0, 0, 0, 0, "Análise de sentimento não está habilitada neste ente.");
    }
}
