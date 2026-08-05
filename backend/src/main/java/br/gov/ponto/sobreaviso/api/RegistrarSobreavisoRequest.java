package br.gov.ponto.sobreaviso.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrarSobreavisoRequest(
        @NotNull UUID vinculoId,
        @NotNull LocalDate data,
        @Positive int minutos,
        String observacao
) {
}
