package br.gov.ponto.ia;

/** Classificação de sentimento de uma lista de comentários: contagens + resumo curto. */
public record SentimentoIa(int positivos, int neutros, int negativos, String resumo) {
}
