package br.gov.ponto.apuracao.domain;

import java.util.List;

/** Resultado da apuracao de um dia (objeto de dominio, independente da API). */
public record ResultadoApuracao(
        int minutosTrabalhados,
        int minutosEsperados,
        boolean diaUtil,
        List<Ocorrencia> ocorrencias
) {
}
