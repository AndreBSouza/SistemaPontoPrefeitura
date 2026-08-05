package br.gov.ponto.common.tenant;

import java.util.UUID;

/**
 * Principal autenticado que carrega o tenant ao qual pertence (ex.: token de dispositivo).
 * O {@link TenantFilter} usa o tenant do principal e ignora o cabeçalho {@code X-Tenant-Id},
 * impedindo que uma requisição autenticada atue sobre outro ente.
 */
public interface TenantAware {
    UUID tenantId();
}
