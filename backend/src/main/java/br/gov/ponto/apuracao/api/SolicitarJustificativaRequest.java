package br.gov.ponto.apuracao.api;

import br.gov.ponto.apuracao.domain.TipoJustificativa;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record SolicitarJustificativaRequest(
        @NotNull UUID vinculoId,
        @NotNull TipoJustificativa tipo,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        String motivo,
        String anexo
) {
}
