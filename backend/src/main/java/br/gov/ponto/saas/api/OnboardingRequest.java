package br.gov.ponto.saas.api;

import br.gov.ponto.tenant.domain.TipoPoder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OnboardingRequest(
        @NotBlank String nome,
        @NotBlank @Pattern(regexp = "[a-z0-9-]{2,60}") String slug,
        @NotNull TipoPoder tipoPoder,
        String lotacaoInicial
) {
}
