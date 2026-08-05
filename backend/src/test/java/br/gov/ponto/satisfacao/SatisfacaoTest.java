package br.gov.ponto.satisfacao;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.me.MeService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class SatisfacaoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private MeService meService;
    @Autowired
    private SatisfacaoService satisfacaoService;

    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente Sat", "ente-sat", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        var s = servidorService.criar(new CriarServidorRequest("80808080808", "Val", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));
        vinculoId = s.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void agregaMediaEDistribuicaoDasAvaliacoes() {
        meService.avaliarSatisfacao(vinculoId, 5, "Ficou muito prático");
        meService.avaliarSatisfacao(vinculoId, 3, null);

        var resumo = satisfacaoService.resumo();
        assertThat(resumo.total()).isEqualTo(2);
        assertThat(resumo.media()).isEqualTo(4.0);
        assertThat(resumo.distribuicao()).containsEntry(5, 1).containsEntry(3, 1).containsEntry(1, 0);
        assertThat(resumo.comentarios()).containsExactly("Ficou muito prático");
    }

    @Test
    void rejeitaNotaForaDaFaixa() {
        assertThatThrownBy(() -> satisfacaoService.registrar(vinculoId, 6, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
