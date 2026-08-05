package br.gov.ponto.relatorios.api;

import java.util.UUID;

public record RelatorioFrequenciaResponse(
        UUID vinculoId,
        String competencia,
        int totalMinutosTrabalhados,
        int totalMinutosEsperados,
        int qtdAtrasos,
        int qtdFaltas,
        int minutosHoraExtra,
        int diasJustificados
) {
}
