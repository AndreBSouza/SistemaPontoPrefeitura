package br.gov.ponto.calendario.domain;

/**
 * Tipo de evento do calendário oficial do município. Todos são dias em que não se
 * espera trabalho (não geram falta na apuração); o tipo serve à categorização.
 */
public enum TipoEventoCalendario {
    /** Feriado municipal/nacional. */
    FERIADO,
    /** Ponto facultativo (expediente dispensado). */
    PONTO_FACULTATIVO,
    /** Abono coletivo por evento (interdição do prédio, luto oficial, etc.). */
    ABONO_COLETIVO
}
