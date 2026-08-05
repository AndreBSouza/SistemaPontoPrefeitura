package br.gov.ponto.tenant.api;

import br.gov.ponto.tenant.domain.TipoPoder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CriarTenantRequest(
        @NotBlank String nome,
        @NotBlank @Pattern(regexp = "[a-z0-9-]{2,60}", message = "slug deve ser kebab-case") String slug,
        @NotNull TipoPoder tipoPoder
) {
}
