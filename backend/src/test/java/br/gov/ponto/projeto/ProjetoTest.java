package br.gov.ponto.projeto;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.projeto.domain.Projeto;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class ProjetoTest {

    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private ProjetoService projetoService;

    private UUID vinculoEstagiario;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente Proj", "ente-proj", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        // Vínculo de estagiário (regime novo, 12.4.7).
        var s = servidorService.criar(new CriarServidorRequest("13131313131", "Lia", null,
                List.of(new CriarVinculoRequest("E-1", Regime.ESTAGIARIO, "Estagiária", 30))));
        vinculoEstagiario = s.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void apropriaHorasEAgregaPorProjeto() {
        Projeto convenio = projetoService.criar("Convênio Saúde 01", "FNS-2026");
        projetoService.apropriar(vinculoEstagiario, convenio.getId(), COMPETENCIA.atDay(3), 240, "Campanha de vacinação");
        projetoService.apropriar(vinculoEstagiario, convenio.getId(), COMPETENCIA.atDay(4), 180, null);

        var rel = projetoService.relatorio(COMPETENCIA);
        assertThat(rel.projetos()).hasSize(1);
        assertThat(rel.projetos().get(0).nome()).isEqualTo("Convênio Saúde 01");
        assertThat(rel.projetos().get(0).fonte()).isEqualTo("FNS-2026");
        assertThat(rel.projetos().get(0).totalMinutos()).isEqualTo(420);
        assertThat(rel.projetos().get(0).lancamentos()).isEqualTo(2);
    }
}
