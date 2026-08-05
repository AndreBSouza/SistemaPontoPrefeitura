package br.gov.ponto.cadastro.domain;

/** Regime juridico do vinculo do servidor. */
public enum Regime {
    ESTATUTARIO,
    CELETISTA,
    COMISSIONADO,
    /** Estagiário (Lei 11.788/2008). */
    ESTAGIARIO,
    /** Contratação temporária (art. 37, IX, CF). */
    TEMPORARIO,
    /** Terceirizado (contrato com empresa prestadora). */
    TERCEIRIZADO
}
