package br.gov.ponto.notificacao;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.auditoria.domain.AuditoriaEvento;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.CompetenciaService;
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
class TrilhaLembreteTest {

    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private CompetenciaService competenciaService;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private LembretePendenciaService lembretePendenciaService;
    @Autowired
    private NotificacaoService notificacaoService;

    private UUID servidorId;
    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente TL", "ente-tl", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        var s = servidorService.criar(new CriarServidorRequest("12121212121", "Otto", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));
        servidorId = s.id();
        vinculoId = s.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void alteracaoDeRegrasGeraTrilhaAntesDepois() {
        UUID orgao = lotacaoService.criar("Secretaria", "SEC").getId();
        lotacaoService.definirRegras(orgao, new RegrasPonto(null, 10, true, null, null, null));

        assertThat(auditoriaService.listar())
                .extracting(AuditoriaEvento::getAcao)
                .contains("REGRAS_ORGAO");
    }

    @Test
    void lembreteDeCienciaNotificaServidorComCompetenciaFechada() {
        competenciaService.fechar(vinculoId, COMPETENCIA); // fechada, sem ciência

        int enviados = lembretePendenciaService.lembrarCienciasPendentes();
        assertThat(enviados).isEqualTo(1);
        assertThat(notificacaoService.listar(servidorId.toString()))
                .extracting(n -> n.getAssunto())
                .contains("Dê ciência do seu espelho");
    }
}
