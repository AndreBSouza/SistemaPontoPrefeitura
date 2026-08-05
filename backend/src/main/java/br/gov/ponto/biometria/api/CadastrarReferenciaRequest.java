package br.gov.ponto.biometria.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CadastrarReferenciaRequest(
        @NotNull UUID servidorId,
        @NotBlank String referencia
) {
}
