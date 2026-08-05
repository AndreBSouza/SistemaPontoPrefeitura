package br.gov.ponto.saas;

import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.saas.api.OnboardingRequest;
import br.gov.ponto.saas.api.OnboardingResponse;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.api.TenantResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Provisiona um novo ente (tenant) e seus parametros iniciais. NAO e uma unica
 * transacao: o tenant e criado primeiro; a lotacao inicial e gravada ja no contexto
 * do novo tenant (RLS), em transacao propria.
 */
@Service
public class OnboardingService {

    private final TenantService tenantService;
    private final LotacaoService lotacaoService;

    public OnboardingService(TenantService tenantService, LotacaoService lotacaoService) {
        this.tenantService = tenantService;
        this.lotacaoService = lotacaoService;
    }

    public OnboardingResponse provisionar(OnboardingRequest request) {
        TenantResponse tenant = tenantService.criar(
                new CriarTenantRequest(request.nome(), request.slug(), request.tipoPoder()));

        String anterior = TenantContext.get();
        TenantContext.set(tenant.id().toString());
        try {
            UUID lotacaoId = null;
            if (request.lotacaoInicial() != null && !request.lotacaoInicial().isBlank()) {
                lotacaoId = lotacaoService.criar(request.lotacaoInicial(), null).getId();
            }
            return new OnboardingResponse(tenant.id(), tenant.slug(), lotacaoId);
        } finally {
            if (anterior != null) {
                TenantContext.set(anterior);
            } else {
                TenantContext.clear();
            }
        }
    }
}
