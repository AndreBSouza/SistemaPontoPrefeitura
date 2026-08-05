package br.gov.ponto.tenant;

import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.api.TenantResponse;
import br.gov.ponto.tenant.domain.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Provisionamento de tenants (operacao do operador SaaS). Tabela sem RLS. */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantResponse criar(CriarTenantRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new ConflitoException("Ja existe ente com o slug " + request.slug());
        }
        Tenant tenant = tenantRepository.save(
                new Tenant(request.nome(), request.slug(), request.tipoPoder()));
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listar() {
        return tenantRepository.findAll().stream().map(TenantResponse::from).toList();
    }
}
