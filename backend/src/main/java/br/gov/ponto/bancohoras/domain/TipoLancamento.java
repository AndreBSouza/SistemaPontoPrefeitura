package br.gov.ponto.bancohoras.domain;

/** Origem do lancamento no banco de horas. */
public enum TipoLancamento {
    APURACAO,
    COMPENSACAO,
    AJUSTE,
    PRESCRICAO,
    /** Saldo inicial trazido de um sistema anterior (migração zero-dor). */
    MIGRACAO
}
