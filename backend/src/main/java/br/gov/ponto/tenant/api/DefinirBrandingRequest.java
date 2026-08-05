package br.gov.ponto.tenant.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Definição da identidade visual do ente. Campos nulos = usa o default do sistema. */
public record DefinirBrandingRequest(
        @Size(max = 60) String nomeApp,
        @Size(max = 500) String logoUrl,
        @Pattern(regexp = "^#?[0-9A-Fa-f]{6}$", message = "cor deve ser hexadecimal (ex.: #1351B4)")
        String corPrimaria,
        @Pattern(regexp = "^#?[0-9A-Fa-f]{6}$", message = "cor deve ser hexadecimal (ex.: #1F6E5C)")
        String corAcento
) {
}
