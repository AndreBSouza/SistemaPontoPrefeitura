package br.gov.ponto.registro.domain;

/** Canal de origem do registro. */
public enum OrigemRegistro {
    MOBILE,
    WEB,
    TOTEM,
    /** Marcação criada por correção ("esqueci de bater" aprovado / correção do RH). */
    AJUSTE
}
