package br.gov.ponto.ia;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.funcionalidade.FuncionalidadeService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Sem provedor de IA (default {@link IaIndisponivel}): tudo responde "indisponível" — mesmo com o flag ligado. */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class IaGatingTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private FuncionalidadeService funcionalidadeService;
    @Autowired
    private AssistenteService assistenteService;
    @Autowired
    private OcrAtestadoService ocrAtestadoService;
    @Autowired
    private ResumoIaService resumoIaService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Ente IA", "ente-ia", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void assistenteIndisponivelPorPadrao() {
        assertThat(assistenteService.perguntar("oi", UUID.randomUUID()).disponivel()).isFalse();
    }

    @Test
    void mesmoComFlagLigadoSemProvedorFicaIndisponivel() {
        funcionalidadeService.definir("IA_ASSISTENTE", true);
        funcionalidadeService.definir("IA_OCR", true);
        funcionalidadeService.definir("IA_RESUMO", true);

        assertThat(assistenteService.perguntar("oi", UUID.randomUUID()).disponivel()).isFalse();
        assertThat(ocrAtestadoService.ler(new byte[]{1, 2, 3}, "image/png").disponivel()).isFalse();
        assertThat(resumoIaService.resumir(YearMonth.of(2026, 6)).disponivel()).isFalse();
    }
}
