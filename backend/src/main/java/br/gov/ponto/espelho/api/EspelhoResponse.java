package br.gov.ponto.espelho.api;

import br.gov.ponto.apuracao.api.ApuracaoDiaResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EspelhoResponse(
        UUID vinculoId,
        String competencia,
        String status,
        Instant cienciaEm,
        int totalMinutosTrabalhados,
        int totalMinutosEsperados,
        List<ApuracaoDiaResponse> dias
) {
}
