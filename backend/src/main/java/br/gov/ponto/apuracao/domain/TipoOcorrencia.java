package br.gov.ponto.apuracao.domain;

/** Tipos de ocorrencia apurados na frequencia. */
public enum TipoOcorrencia {
    ATRASO,
    SAIDA_ANTECIPADA,
    FALTA,
    HORA_EXTRA,
    ADICIONAL_NOTURNO;

    /**
     * Ocorrência que gera desconto/penalidade ao servidor (atraso, falta, saída antecipada).
     * Usado pelo modo adaptação, que suprime estas penalidades no período inicial.
     */
    public boolean penalizaServidor() {
        return this == ATRASO || this == SAIDA_ANTECIPADA || this == FALTA;
    }
}
