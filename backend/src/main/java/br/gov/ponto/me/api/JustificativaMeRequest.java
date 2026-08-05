package br.gov.ponto.me.api;

import br.gov.ponto.apuracao.domain.TipoJustificativa;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Solicitação de justificativa do app: vinculo derivado do dispositivo. */
public record JustificativaMeRequest(
        @NotNull TipoJustificativa tipo,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        String motivo,
        String anexo
) {
}
