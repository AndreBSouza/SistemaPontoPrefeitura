package br.gov.ponto.me;

import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.me.api.CarteiraResponse;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class CarteiraTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private CarteiraService carteiraService;

    private UUID tenantId;
    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Prefeitura X", "pref-x", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var s = servidorService.criar(new CriarServidorRequest("90909090909", "Marta", null,
                List.of(new CriarVinculoRequest("M-7", Regime.ESTATUTARIO, "Fiscal", 40))));
        vinculoId = s.vinculos().get(0).id();
        UUID orgao = lotacaoService.criar("Secretaria de Obras", "SEOB").getId();
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId).orElseThrow();
        v.setLotacaoId(orgao);
        vinculoRepository.save(v);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void carteiraTrazDadosDoVinculoEDoEnte() {
        CarteiraResponse c = carteiraService.carteira(vinculoId);
        assertThat(c.nome()).isEqualTo("Marta");
        assertThat(c.cpf()).isEqualTo("90909090909");
        assertThat(c.matricula()).isEqualTo("M-7");
        assertThat(c.cargo()).isEqualTo("Fiscal");
        assertThat(c.regime()).isEqualTo("ESTATUTARIO");
        assertThat(c.orgao()).isEqualTo("Secretaria de Obras");
        assertThat(c.ente()).isEqualTo("Prefeitura X");
        assertThat(c.corPrimaria()).isNotBlank();
    }
}
