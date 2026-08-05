package br.gov.ponto.apuracao.api;

import br.gov.ponto.apuracao.domain.TipoOcorrencia;

public record OcorrenciaResponse(
        TipoOcorrencia tipo,
        int minutos
) {
}
