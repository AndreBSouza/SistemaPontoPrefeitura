package br.gov.ponto.satisfacao;

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

import static org.assertj.core.api.Assertions.assertThat;

/** Sem provedor de IA, a análise de sentimento fica "indisponível" — mesmo com o flag ligado. */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class SentimentoGatingTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private FuncionalidadeService funcionalidadeService;
    @Autowired
    private SentimentoService sentimentoService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Ente S", "ente-s", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void desligadoPorPadrao() {
        assertThat(sentimentoService.analisar().disponivel()).isFalse();
    }

    @Test
    void ligadoSemProvedorFicaIndisponivel() {
        funcionalidadeService.definir("IA_SENTIMENTO", true);
        assertThat(sentimentoService.analisar().disponivel()).isFalse();
    }
}
