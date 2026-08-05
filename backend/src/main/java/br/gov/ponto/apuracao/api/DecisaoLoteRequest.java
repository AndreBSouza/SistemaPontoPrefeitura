package br.gov.ponto.apuracao.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Decisão em lote de justificativas (aprovar/recusar várias de uma vez). */
public record DecisaoLoteRequest(
        @NotEmpty List<UUID> ids,
        String motivoDecisao
) {
}
