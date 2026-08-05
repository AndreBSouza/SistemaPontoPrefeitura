package br.gov.ponto.me;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.api.AtivacaoResponse;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tempo.TempoMunicipal;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caminho feliz ponta a ponta (HTTP + autenticação por dispositivo + /api/me):
 * ativação por código → 4 batidas pelo botão único (tipos deduzidos) → comprovantes → espelho.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class FluxoCompletoE2eTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private JornadaService jornadaService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private AtivacaoService ativacaoService;

    private String deviceToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente E2E", "ente-e2e", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "55555555555", "E2E", null,
                List.of(new CriarVinculoRequest("M-E2E", Regime.ESTATUTARIO, "Analista", 40))));
        UUID vinculoId = servidor.vinculos().get(0).id();

        // Jornada COM intervalo → o botão único deduz ciclo de 4 fases.
        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Integral", TipoJornada.FIXA, 2400, 5, 60)).id();
        var orgao = lotacaoService.criar("Secretaria E2E", "SE2E");
        lotacaoService.definirRegras(orgao.getId(), new RegrasPonto(jornadaId, null, true, null, null, null));
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoId, tenant).orElseThrow();
        v.setLotacaoId(orgao.getId());
        vinculoRepository.save(v);

        GerarCodigoResponse codigo = ativacaoService.gerar(vinculoId, null);
        TenantContext.clear();
        AtivacaoResponse ativacao = ativacaoService.ativar(codigo.codigo(), "Aparelho E2E");
        deviceToken = ativacao.deviceToken();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void ativacaoBaterDeduzindoTiposEEspelho() throws Exception {
        bater("e2e-1", "ENTRADA");
        bater("e2e-2", "INTERVALO_INICIO");
        bater("e2e-3", "INTERVALO_FIM");
        bater("e2e-4", "SAIDA");

        // Comprovantes do próprio servidor (derivado do device token).
        mockMvc.perform(get("/api/me/comprovantes").header("X-Device-Token", deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        // Espelho do mês corrente.
        String competencia = YearMonth.now(TempoMunicipal.ZONE).toString();
        mockMvc.perform(get("/api/me/espelho")
                        .header("X-Device-Token", deviceToken)
                        .param("competencia", competencia))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competencia").value(competencia));
    }

    private void bater(String idempotencyKey, String tipoEsperado) throws Exception {
        mockMvc.perform(post("/api/me/bater")
                        .header("X-Device-Token", deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"offline\":false,\"idempotencyKey\":\"" + idempotencyKey + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value(tipoEsperado));
    }
}
