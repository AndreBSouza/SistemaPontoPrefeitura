package br.gov.ponto.ativacao;

import br.gov.ponto.common.tenant.TenantAware;

import java.util.UUID;

/** Identidade do dispositivo autenticado (princípio da autenticação por token). */
public record DispositivoPrincipal(UUID tenantId, UUID vinculoId, UUID dispositivoId)
        implements TenantAware {
}
