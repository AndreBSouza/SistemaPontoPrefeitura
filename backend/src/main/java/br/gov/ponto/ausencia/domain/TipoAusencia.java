package br.gov.ponto.ausencia.domain;

/** Tipo de ausência programada do servidor (não gera falta na apuração no período). */
public enum TipoAusencia {
    FERIAS,
    LICENCA_MEDICA,
    LICENCA_MATERNIDADE,
    LICENCA_PATERNIDADE,
    LICENCA_PREMIO,
    LICENCA_NOJO,
    /** Deslocamento a serviço (viagem com diária). */
    VIAGEM,
    /** Capacitação/treinamento fora do posto. */
    CAPACITACAO,
    OUTRA
}
