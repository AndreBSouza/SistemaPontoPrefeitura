package br.gov.ponto.relatorios;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class AnomaliaGatingTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private FuncionalidadeService funcionalidadeService;
    @Autowired
    private AnomaliaService anomaliaService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Ente A", "ente-a", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void desligadaPorPadraoRetornaHabilitadaFalse() {
        var resp = anomaliaService.detectar(YearMonth.of(2026, 6));
        assertThat(resp.habilitada()).isFalse();
        assertThat(resp.anomalias()).isEmpty();
    }

    @Test
    void ligadaRodaSemErroRetornaHabilitadaTrue() {
        funcionalidadeService.definir("ANOMALIAS", true);
        var resp = anomaliaService.detectar(YearMonth.of(2026, 6));
        assertThat(resp.habilitada()).isTrue();
        assertThat(resp.anomalias()).isNotNull();
    }
}
