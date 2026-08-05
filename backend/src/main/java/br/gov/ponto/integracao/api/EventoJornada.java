package br.gov.ponto.integracao.api;

public record EventoJornada(
        String matricula,
        String regime,
        int totalMinutosTrabalhados
) {
}
