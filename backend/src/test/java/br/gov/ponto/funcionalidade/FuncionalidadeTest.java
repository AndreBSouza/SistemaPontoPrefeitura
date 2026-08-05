package br.gov.ponto.funcionalidade;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.funcionalidade.domain.Funcionalidade;
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

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class FuncionalidadeTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private FuncionalidadeService funcionalidadeService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Ente F", "ente-f", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void desligadaPorPadraoELigaAoDefinir() {
        assertThat(funcionalidadeService.habilitada(Funcionalidade.ANOMALIAS)).isFalse();

        funcionalidadeService.definir("ANOMALIAS", true);
        assertThat(funcionalidadeService.habilitada(Funcionalidade.ANOMALIAS)).isTrue();

        funcionalidadeService.definir("ANOMALIAS", false);
        assertThat(funcionalidadeService.habilitada(Funcionalidade.ANOMALIAS)).isFalse();
    }

    @Test
    void listarTrazTodasComEstado() {
        funcionalidadeService.definir("IA_OCR", true);
        var lista = funcionalidadeService.listar();
        assertThat(lista).hasSize(Funcionalidade.values().length);
        assertThat(lista).anyMatch(f -> f.chave().equals("IA_OCR") && f.habilitado());
        assertThat(lista).anyMatch(f -> f.chave().equals("ANOMALIAS") && !f.habilitado());
    }
}
