package br.gov.ponto.common.tenant;

import java.util.UUID;

/**
 * Contexto de tenant (ente municipal) corrente, propagado por requisicao.
 *
 * <p>Na fundacao o tenant chega via cabecalho HTTP ({@link TenantFilter}). Quando o
 * IAM (Keycloak) estiver integrado, sera resolvido a partir do claim do token JWT.
 * O isolamento efetivo dos dados se dara por RLS no PostgreSQL + filtro de aplicacao
 * (ver spec multi-tenancy-iam).</p>
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Tenant corrente como UUID, exigindo que esteja definido.
     *
     * @throws IllegalStateException se nenhum tenant estiver no contexto
     */
    public static UUID requireCurrent() {
        String value = get();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Tenant nao definido no contexto da requisicao");
        }
        return UUID.fromString(value.trim());
    }
}
