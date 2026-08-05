package br.gov.ponto.espelho.api;

import br.gov.ponto.espelho.domain.Competencia;
import br.gov.ponto.espelho.domain.StatusCompetencia;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record CompetenciaResponse(
        UUID id,
        UUID vinculoId,
        String competencia,
        StatusCompetencia status,
        Instant fechadoEm,
        Instant reabertoEm,
        String motivoReabertura,
        Instant cienciaEm
) {
    public static CompetenciaResponse from(Competencia c) {
        return new CompetenciaResponse(
                c.getId(), c.getVinculoId(),
                c.getAnoMes().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                c.getStatus(), c.getFechadoEm(), c.getReabertoEm(),
                c.getMotivoReabertura(), c.getCienciaEm());
    }
}
