package br.gov.ponto.me;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.api.AtivacaoResponse;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticação por token de dispositivo na cadeia do Spring Security (perfil seguro,
 * como em produção): o app acessa /api/me, mas não os endpoints administrativos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class MeAuthTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private AtivacaoService ativacaoService;

    private UUID tenant;
    private String deviceToken;
    private UUID dispositivoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenant = tenantService.criar(new CriarTenantRequest("Ente Me", "ente-me", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        var servidor = servidorService.criar(new CriarServidorRequest(
                "55555555555", "Nina", null,
                List.of(new CriarVinculoRequest("M-2", Regime.ESTATUTARIO, "Aux", 40))));
        UUID vinculoId = servidor.vinculos().get(0).id();
        GerarCodigoResponse codigo = ativacaoService.gerar(vinculoId, null);

        TenantContext.clear();
        AtivacaoResponse ativacao = ativacaoService.ativar(codigo.codigo(), "Telefone");
        deviceToken = ativacao.deviceToken();
        dispositivoId = ativacao.dispositivoId();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void deviceTokenAcessaApiMe() throws Exception {
        mockMvc.perform(get("/api/me/comprovantes").header("X-Device-Token", deviceToken))
                .andExpect(status().isOk());
    }

    @Test
    void lgpdAutoatendimentoPeloApp() throws Exception {
        // Direito de acesso: o titular exporta os próprios dados.
        mockMvc.perform(get("/api/me/lgpd/dados").header("X-Device-Token", deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nina"));

        // Concede o consentimento de biometria...
        mockMvc.perform(post("/api/me/lgpd/consentimento").header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finalidade\":\"BIOMETRIA\",\"concedido\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concedido").value(true));

        // ...e a consulta reflete.
        mockMvc.perform(get("/api/me/lgpd/consentimento?finalidade=BIOMETRIA")
                        .header("X-Device-Token", deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.concedido").value(true));
    }

    @Test
    void configDoAppRetornaVerificacao() throws Exception {
        mockMvc.perform(get("/api/me/config").header("X-Device-Token", deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificacaoObrigatoria").value(false));
    }

    @Test
    void brandingDoAppAplicaDefaults() throws Exception {
        mockMvc.perform(get("/api/me/branding").header("X-Device-Token", deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeApp").value("Ponto Municipal"))
                .andExpect(jsonPath("$.corPrimaria").value("#1351B4"));
    }

    @Test
    void deviceTokenNaoAcessaEndpointAdmin() throws Exception {
        mockMvc.perform(get("/api/jornadas").header("X-Device-Token", deviceToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void semCredencialRetorna401() throws Exception {
        mockMvc.perform(get("/api/me/comprovantes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/api/me/comprovantes").header("X-Device-Token", "token-inexistente"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dispositivoRevogadoPerdeAcesso() throws Exception {
        TenantContext.set(tenant.toString());
        ativacaoService.revogar(dispositivoId);
        TenantContext.clear();

        mockMvc.perform(get("/api/me/comprovantes").header("X-Device-Token", deviceToken))
                .andExpect(status().isUnauthorized());
    }
}
