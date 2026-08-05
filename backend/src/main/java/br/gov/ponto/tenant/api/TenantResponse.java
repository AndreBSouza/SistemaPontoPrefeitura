package br.gov.ponto.tenant.api;

import br.gov.ponto.tenant.domain.Tenant;
import br.gov.ponto.tenant.domain.TipoPoder;

import java.util.UUID;

public record TenantResponse(
        UUID id,
        String nome,
        String slug,
        TipoPoder tipoPoder,
        boolean ativo
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.getId(), t.getNome(), t.getSlug(), t.getTipoPoder(), t.isAtivo());
    }
}
