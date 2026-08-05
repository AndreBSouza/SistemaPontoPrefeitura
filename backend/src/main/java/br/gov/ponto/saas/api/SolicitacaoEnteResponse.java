package br.gov.ponto.saas.api;

import br.gov.ponto.saas.domain.SolicitacaoEnte;
import br.gov.ponto.saas.domain.StatusSolicitacao;
import br.gov.ponto.tenant.domain.TipoPoder;

import java.time.Instant;
import java.util.UUID;

public record SolicitacaoEnteResponse(
        UUID id,
        String nome,
        String slug,
        TipoPoder tipoPoder,
        String responsavelNome,
        String responsavelEmail,
        StatusSolicitacao status,
        UUID tenantId,
        Instant criadoEm
) {
    public static SolicitacaoEnteResponse from(SolicitacaoEnte s) {
        return new SolicitacaoEnteResponse(s.getId(), s.getNome(), s.getSlug(), s.getTipoPoder(),
                s.getResponsavelNome(), s.getResponsavelEmail(), s.getStatus(), s.getTenantId(), s.getCriadoEm());
    }
}
