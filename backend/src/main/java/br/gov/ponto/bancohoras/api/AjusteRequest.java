package br.gov.ponto.bancohoras.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AjusteRequest(
        @NotNull UUID vinculoId,
        @NotNull LocalDate data,
        int minutos,
        String descricao
) {
}
