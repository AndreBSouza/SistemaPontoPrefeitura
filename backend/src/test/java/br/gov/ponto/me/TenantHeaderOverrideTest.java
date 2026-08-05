package br.gov.ponto.me;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.BrandingService;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.api.DefinirBrandingRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressão de isolamento multi-tenant: uma requisição autenticada por token de DISPOSITIVO opera
 * SEMPRE no tenant do dispositivo — o cabeçalho {@code X-Tenant-Id} não pode sobrescrever o tenant
 * do principal autenticado. Antes da correção, um device do ente A com {@code X-Tenant-Id: <B>}
 * lia o branding do ente B (leitura cross-tenant). Ver {@code TenantFilter.fromPrincipal()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class TenantHeaderOverrideTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private AtivacaoService ativacaoService;
    @Autowired
    private BrandingService brandingService;

    private String deviceTokenA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        // Ente A: branding próprio + dispositivo autenticado.
        UUID tenantA = tenantService.criar(new CriarTenantRequest("Ente A", "ente-a", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantA.toString());
        brandingService.definir(new DefinirBrandingRequest("App do Ente A", null, "#111111", "#222222"));
        var servidor = servidorService.criar(new CriarServidorRequest(
                "55555555555", "Ana", null,
                List.of(new CriarVinculoRequest("M-A", Regime.ESTATUTARIO, "Aux", 40))));
        GerarCodigoResponse codigo = ativacaoService.gerar(servidor.vinculos().get(0).id(), null);
        TenantContext.clear();
        deviceTokenA = ativacaoService.ativar(codigo.codigo(), "Telefone").deviceToken();

        // Ente B: branding distinto.
        tenantB = tenantService.criar(new CriarTenantRequest("Ente B", "ente-b", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantB.toString());
        brandingService.definir(new DefinirBrandingRequest("App do Ente B", null, "#333333", "#444444"));
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void deviceTokenIgnoraXTenantIdApontandoParaOutroEnte() throws Exception {
        // Device do A + cabeçalho do B: deve operar em A (branding de A), NUNCA o de B.
        mockMvc.perform(get("/api/me/branding")
                        .header("X-Device-Token", deviceTokenA)
                        .header("X-Tenant-Id", tenantB.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeApp").value("App do Ente A"));
    }

    @Test
    void semCabecalhoUsaOTenantDoDispositivo() throws Exception {
        mockMvc.perform(get("/api/me/branding").header("X-Device-Token", deviceTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeApp").value("App do Ente A"));
    }
}
