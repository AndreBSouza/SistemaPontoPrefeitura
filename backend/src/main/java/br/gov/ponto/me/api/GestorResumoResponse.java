package br.gov.ponto.me.api;

/** Resumo de gestão para o app do servidor: se é chefia e quantas pendências do time há. */
public record GestorResumoResponse(
        boolean souGestor,
        int pendentes
) {
}
