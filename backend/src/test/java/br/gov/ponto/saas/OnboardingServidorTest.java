package br.gov.ponto.saas;

import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.saas.api.OnboardingServidorResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class OnboardingServidorTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private OnboardingServidorService onboardingServidorService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        TenantContext.set(tenantService.criar(
                new CriarTenantRequest("Ente Onb", "ente-onb", TipoPoder.EXECUTIVO)).id().toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void onboardCriaServidorEJaGeraCodigoDeAtivacao() {
        OnboardingServidorResponse r = onboardingServidorService.onboard(new CriarServidorRequest(
                "14141414141", "Nara", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));

        assertThat(r.servidor().vinculos()).hasSize(1);
        assertThat(r.codigoAtivacao()).isNotNull();
        assertThat(r.codigoAtivacao().codigo()).isNotBlank();
        assertThat(r.codigoAtivacao().vinculoId()).isEqualTo(r.servidor().vinculos().get(0).id());
    }
}
