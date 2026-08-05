package br.gov.ponto.jornada.api;

import br.gov.ponto.jornada.domain.Escala;

import java.time.LocalDate;
import java.util.UUID;

public record EscalaResponse(
        UUID id,
        UUID vinculoId,
        UUID jornadaId,
        LocalDate dataInicio,
        LocalDate dataFim
) {
    public static EscalaResponse from(Escala e) {
        return new EscalaResponse(e.getId(), e.getVinculoId(), e.getJornadaId(),
                e.getDataInicio(), e.getDataFim());
    }
}
