package br.gov.ponto.ausencia.api;

import br.gov.ponto.ausencia.domain.AusenciaProgramada;
import br.gov.ponto.ausencia.domain.TipoAusencia;

import java.time.LocalDate;
import java.util.UUID;

public record AusenciaResponse(
        UUID id,
        UUID vinculoId,
        TipoAusencia tipo,
        LocalDate dataInicio,
        LocalDate dataFim,
        int dias,
        String observacao
) {
    public static AusenciaResponse from(AusenciaProgramada a) {
        return new AusenciaResponse(a.getId(), a.getVinculoId(), a.getTipo(),
                a.getDataInicio(), a.getDataFim(), a.dias(), a.getObservacao());
    }
}
