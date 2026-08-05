package br.gov.ponto.common.tenant;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Blindagem do isolamento entre entes: o cabeçalho X-Tenant-Id NUNCA sobrepõe o tenant de uma
 * requisição autenticada — nem quando o token não traz o claim tenant_id.
 */
class TenantFilterTest {

    private final TenantFilter filter = new TenantFilter();

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    /** Executa o filtro e captura o tenant que ficou no contexto DURANTE a cadeia. */
    private String tenantVistoCom(String headerTenant) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (headerTenant != null) {
            req.addHeader(TenantFilter.TENANT_HEADER, headerTenant);
        }
        String[] visto = new String[1];
        FilterChain chain = (r, s) -> visto[0] = TenantContext.get();
        filter.doFilter(req, new MockHttpServletResponse(), chain);
        return visto[0];
    }

    private static void autenticarComJwt(String tenantIdClaim) {
        Jwt.Builder b = Jwt.withTokenValue("token").header("alg", "none").subject("admin");
        if (tenantIdClaim != null) {
            b.claim("tenant_id", tenantIdClaim);
        }
        Authentication auth = new JwtAuthenticationToken(b.build(),
                List.of(new SimpleGrantedAuthority("ROLE_rh")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void adminComClaimUsaOTenantDoTokenEIgnoraOCabecalho() throws Exception {
        autenticarComJwt("ente-A");
        assertThat(tenantVistoCom("ente-B")).isEqualTo("ente-A");
    }

    @Test
    void adminSemClaimNaoCaiNoCabecalho() throws Exception {
        autenticarComJwt(null);
        // O admin não pode pivotar para outro ente pelo header — contexto fica vazio (RLS barra).
        assertThat(tenantVistoCom("ente-B")).isNull();
    }

    @Test
    void dispositivoUsaOTenantDoPrincipalEIgnoraOCabecalho() throws Exception {
        UUID tenant = UUID.randomUUID();
        TenantAware principal = () -> tenant;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_servidor"))));
        assertThat(tenantVistoCom("ente-B")).isEqualTo(tenant.toString());
    }

    @Test
    void semAutenticacaoUsaOCabecalho() throws Exception {
        // Totem/dev (SecurityContext vazio) — aí sim o header define o tenant.
        assertThat(tenantVistoCom("ente-C")).isEqualTo("ente-C");
    }

    @Test
    void anonimoUsaOCabecalho() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anon", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThat(tenantVistoCom("ente-D")).isEqualTo("ente-D");
    }
}
