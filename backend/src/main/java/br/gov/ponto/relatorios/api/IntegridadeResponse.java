package br.gov.ponto.relatorios.api;

/** Resultado da verificação da cadeia de integridade (hash-chain) dos registros. */
public record IntegridadeResponse(
        boolean integra,
        int totalRegistros,
        Long nsrRompido,
        String detalhe
) {
}
