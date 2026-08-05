package br.gov.ponto.saas;

import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.saas.api.SolicitacaoEnteResponse;
import br.gov.ponto.saas.api.SolicitarEnteRequest;
import br.gov.ponto.saas.domain.StatusSolicitacao;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class SolicitacaoEnteTest {

    @Autowired
    private SolicitacaoEnteService service;

    @BeforeEach
    void setUp() {
        TenantContext.clear(); // fluxo é pré-tenant (global)
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private SolicitarEnteRequest req(String nome, String slug) {
        return new SolicitarEnteRequest(nome, slug, TipoPoder.EXECUTIVO, "Maria Responsável", "maria@" + slug + ".gov.br");
    }

    @Test
    void solicitarFicaPendenteEAprovarProvisionaOTenant() {
        SolicitacaoEnteResponse sol = service.solicitar(req("Município X", "municipio-x"));
        assertThat(sol.status()).isEqualTo(StatusSolicitacao.PENDENTE);
        assertThat(sol.tenantId()).isNull(); // ainda não virou tenant
        assertThat(service.listarPendentes()).hasSize(1);

        var provisionado = service.aprovar(sol.id());
        assertThat(provisionado.tenantId()).isNotNull();
        assertThat(provisionado.slug()).isEqualTo("municipio-x");
        assertThat(provisionado.lotacaoId()).isNotNull(); // lotação inicial criada no novo tenant
        assertThat(service.listarPendentes()).isEmpty();
    }

    @Test
    void slugComSolicitacaoPendenteNaoPodeDuplicar() {
        service.solicitar(req("Município A", "dup"));
        assertThatThrownBy(() -> service.solicitar(req("Município B", "dup")))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void naoAprovaDuasVezes() {
        var sol = service.solicitar(req("Município Y", "municipio-y"));
        service.aprovar(sol.id());
        assertThatThrownBy(() -> service.aprovar(sol.id())).isInstanceOf(ConflitoException.class);
    }
}
