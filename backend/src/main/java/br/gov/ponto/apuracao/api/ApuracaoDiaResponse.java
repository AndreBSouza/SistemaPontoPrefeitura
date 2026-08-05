package br.gov.ponto.apuracao.api;

import br.gov.ponto.apuracao.domain.ApuracaoDia;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApuracaoDiaResponse(
        UUID vinculoId,
        LocalDate data,
        int minutosTrabalhados,
        int minutosEsperados,
        boolean diaUtil,
        boolean justificado,
        boolean emAdaptacao,
        List<OcorrenciaResponse> ocorrencias
) {
    public static ApuracaoDiaResponse from(ApuracaoDia dia) {
        List<OcorrenciaResponse> ocorrencias = dia.ocorrencias().stream()
                .map(o -> new OcorrenciaResponse(o.tipo(), o.minutos()))
                .toList();
        return new ApuracaoDiaResponse(dia.vinculoId(), dia.data(), dia.minutosTrabalhados(),
                dia.minutosEsperados(), dia.diaUtil(), dia.justificado(), dia.emAdaptacao(), ocorrencias);
    }
}
