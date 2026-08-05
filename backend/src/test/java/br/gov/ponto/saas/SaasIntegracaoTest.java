package br.gov.ponto.saas;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.integracao.EsocialService;
import br.gov.ponto.integracao.FolhaService;
import br.gov.ponto.notificacao.NotificacaoService;
import br.gov.ponto.notificacao.domain.CanalNotificacao;
import br.gov.ponto.saas.api.OnboardingRequest;
import br.gov.ponto.saas.api.OnboardingResponse;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class SaasIntegracaoTest {

    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private OnboardingService onboardingService;
    @Autowired
    private FolhaService folhaService;
    @Autowired
    private EsocialService esocialService;
    @Autowired
    private NotificacaoService notificacaoService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente S", "ente-s", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        servidorService.criar(new CriarServidorRequest(
                "10101010101", "Karla", null,
                List.of(new CriarVinculoRequest("M-CLT", Regime.CELETISTA, "Operaria", 44))));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void onboardingProvisionaTenantComLotacao() {
        OnboardingResponse resp = onboardingService.provisionar(
                new OnboardingRequest("Nova Prefeitura", "nova-pref", TipoPoder.EXECUTIVO, "Gabinete"));
        assertThat(resp.tenantId()).isNotNull();
        assertThat(resp.lotacaoId()).isNotNull();
    }

    @Test
    void exportacaoFolhaEEsocial() {
        String csv = folhaService.exportarCsv(COMPETENCIA);
        assertThat(csv).contains("matricula").contains("M-CLT");

        var esocial = esocialService.gerarEventosJornada(COMPETENCIA);
        assertThat(esocial.eventos()).hasSize(1);
        assertThat(esocial.eventos().get(0).regime()).isEqualTo("CELETISTA");
    }

    @Test
    void notificacaoEnviaERegistra() {
        notificacaoService.enviar("karla@ente.gov.br", "Lembrete", "Registre seu ponto", CanalNotificacao.PUSH);
        assertThat(notificacaoService.listar("karla@ente.gov.br")).hasSize(1);
    }
}
