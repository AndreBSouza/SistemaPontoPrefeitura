package br.gov.ponto.espelho.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Fechamento de competência em lote para vários vínculos de uma vez. */
public record FecharLoteRequest(
        @NotEmpty List<UUID> vinculoIds
) {
}
