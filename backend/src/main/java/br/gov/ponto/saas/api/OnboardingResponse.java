package br.gov.ponto.saas.api;

import java.util.UUID;

public record OnboardingResponse(
        UUID tenantId,
        String slug,
        UUID lotacaoId
) {
}
