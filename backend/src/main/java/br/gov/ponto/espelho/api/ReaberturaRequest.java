package br.gov.ponto.espelho.api;

import jakarta.validation.constraints.NotBlank;

public record ReaberturaRequest(
        @NotBlank String motivo
) {
}
