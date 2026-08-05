package br.gov.ponto.ativacao;

import br.gov.ponto.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Autentica o app por token de dispositivo ({@code X-Device-Token}) dentro da cadeia
 * do Spring Security: quando presente e valido, popula o {@link SecurityContextHolder}
 * com {@link DispositivoAuthentication} (ROLE_servidor) e o {@link TenantContext}.
 * Token ausente → segue o fluxo normal (JWT do Keycloak / dev). Token revogado/invalido → 401.
 *
 * <p>Vale tanto no perfil dev (API aberta) quanto em producao (resource server).</p>
 */
public class DeviceTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String DEVICE_HEADER = "X-Device-Token";

    private final AtivacaoService ativacaoService;

    public DeviceTokenAuthenticationFilter(AtivacaoService ativacaoService) {
        this.ativacaoService = ativacaoService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader(DEVICE_HEADER);
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        Optional<DispositivoPrincipal> dispositivo = ativacaoService.autenticarPorToken(token);
        if (dispositivo.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Dispositivo nao reconhecido ou revogado");
            return;
        }
        DispositivoPrincipal principal = dispositivo.get();
        SecurityContextHolder.getContext().setAuthentication(new DispositivoAuthentication(principal));
        TenantContext.set(principal.tenantId().toString());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
