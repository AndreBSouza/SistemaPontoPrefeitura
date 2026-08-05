package br.gov.ponto.biometria.api;

/** Resultado da verificacao biometrica 1:1. */
public record VerificacaoResponse(
        boolean corresponde,
        double similaridade,
        double limiar
) {
}
