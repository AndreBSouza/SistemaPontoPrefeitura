package br.gov.ponto.apuracao.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resultado de dominio da apuracao de um dia para um vinculo, independente da camada REST.
 * Consumido por banco de horas, espelho e relatorios; a API o converte em ApuracaoDiaResponse.
 */
public record ApuracaoDia(
        UUID vinculoId,
        LocalDate data,
        int minutosTrabalhados,
        int minutosEsperados,
        boolean diaUtil,
        boolean justificado,
        boolean emAdaptacao,
        List<Ocorrencia> ocorrencias
) {
}
