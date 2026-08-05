package br.gov.ponto.common.security;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private JornadaService jornadaService;

    private UUID tenant;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenant = tenantService.criar(
                new CriarTenantRequest("Ente Seg", "ente-seg", TipoPoder.EXECUTIVO)).id();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/jornadas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleSemPermissaoRetorna403() throws Exception {
        mockMvc.perform(get("/api/jornadas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_servidor"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rhComTenantRetorna200() throws Exception {
        // O tenant vem do claim tenant_id do token (produção), não do cabeçalho.
        mockMvc.perform(get("/api/jornadas")
                        .with(jwt()
                                .jwt(j -> j.claim("tenant_id", tenant.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_rh"))))
                .andExpect(status().isOk());
    }

    @Test
    void adminAutenticadoNaoUsaCabecalhoParaTrocarDeEnte() throws Exception {
        // Blindagem cross-tenant: token SEM claim tenant_id + X-Tenant-Id no cabeçalho NÃO resolve
        // o ente. O acesso é NEGADO (400 do handler de IllegalStateException "Tenant nao definido")
        // e, o que mais importa, NENHUM dado do ente do cabeçalho é devolvido.
        String corpo = mockMvc.perform(get("/api/jornadas")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_rh")))
                        .header("X-Tenant-Id", tenant.toString()))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain(tenant.toString());
    }

    @Test
    void tokenDeUmEnteNaoEnxergaOutroEnteMesmoComCabecalho() throws Exception {
        // O cenário do vazamento: admin do ente A tenta operar sobre o ente B pelo cabeçalho.
        UUID enteB = tenantService.criar(
                new CriarTenantRequest("Ente B", "ente-b-seg", TipoPoder.EXECUTIVO)).id();
        TenantContext.clear();

        // Cria uma jornada no ente B para haver algo a vazar.
        TenantContext.set(enteB.toString());
        jornadaService.criar(new CriarJornadaRequest("Jornada do B", TipoJornada.FIXA, 2400, 10, 0));
        TenantContext.clear();

        String corpo = mockMvc.perform(get("/api/jornadas")
                        .with(jwt()
                                .jwt(j -> j.claim("tenant_id", tenant.toString())) // token do ente A
                                .authorities(new SimpleGrantedAuthority("ROLE_rh")))
                        .header("X-Tenant-Id", enteB.toString()))                  // cabeçalho do ente B
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Responde com as jornadas do ente A (vazio) — nunca as do ente B.
        assertThat(corpo).doesNotContain("Jornada do B");
    }
}
