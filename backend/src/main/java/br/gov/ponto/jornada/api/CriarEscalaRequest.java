package br.gov.ponto.jornada.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CriarEscalaRequest(
        @NotNull UUID vinculoId,
        @NotNull UUID jornadaId,
        @NotNull LocalDate dataInicio,
        LocalDate dataFim
) {
}
