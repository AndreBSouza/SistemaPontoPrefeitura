package br.gov.ponto.common.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Resolve o tenant da requisicao e o disponibiliza no {@link TenantContext}.
 *
 * <p>Ordem de resolucao: tenant do PRINCIPAL autenticado — claim {@code tenant_id} do token JWT
 * (admin) ou {@link TenantAware#tenantId()} do token de dispositivo (servidor). O cabecalho
 * {@code X-Tenant-Id} SO e considerado quando a requisicao NAO esta autenticada (totem/dev).</p>
 *
 * <p><b>Isolamento entre entes:</b> uma requisicao autenticada NUNCA usa o cabecalho — nem quando
 * o token nao traz {@code tenant_id} (ex.: claim mal configurado, ou papel {@code operador} que
 * atua sobre tabelas globais). Do contrario um admin do ente A poderia enviar {@code X-Tenant-Id:
 * B} e operar sobre o ente B. Se o token deveria trazer o tenant e nao traz, o contexto fica
 * VAZIO e a RLS do Postgres barra o acesso (fail-closed), em vez de vazar dados de outro ente.
 * Roda apos o filtro de autenticacao do Spring Security, entao o {@link SecurityContextHolder}
 * ja esta populado.</p>
 */
@Component
@Order(1)
public class TenantFilter implements Filter {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // 1) Principal autenticado define o tenant.
            String tenant = fromPrincipal(auth);
            // 2) O cabeçalho X-Tenant-Id só vale para requisições NÃO autenticadas (totem/dev).
            //    Requisição autenticada nunca cai no cabeçalho — mesmo sem tenant no token.
            if (tenant == null && !autenticado(auth) && request instanceof HttpServletRequest http) {
                String header = http.getHeader(TENANT_HEADER);
                if (header != null && !header.isBlank()) {
                    tenant = header.trim();
                }
            }
            if (tenant != null) {
                TenantContext.set(tenant);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /** Há um principal realmente autenticado (não anônimo)? */
    private static boolean autenticado(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    /** Tenant derivado do principal autenticado (JWT do admin ou token de dispositivo). */
    private String fromPrincipal(Authentication auth) {
        if (auth == null) {
            return null;
        }
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String tenant = jwtAuth.getToken().getClaimAsString("tenant_id");
            if (tenant != null && !tenant.isBlank()) {
                return tenant.trim();
            }
            return null;
        }
        if (auth.getPrincipal() instanceof TenantAware tenantAware) {
            return tenantAware.tenantId().toString();
        }
        return null;
    }
}
