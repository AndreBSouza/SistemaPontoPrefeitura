package br.gov.ponto.bancohoras.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record CompensarRequest(
        @NotNull UUID vinculoId,
        @Positive int minutos,
        @NotNull LocalDate data,
        String descricao
) {
}
