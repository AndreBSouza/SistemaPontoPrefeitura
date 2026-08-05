package br.gov.ponto.integracao.api;

import java.util.List;

public record EsocialJornadaResponse(
        String competencia,
        List<EventoJornada> eventos
) {
}
