package br.gov.ponto.tenant;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.api.BrandingResponse;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.api.DefinirBrandingRequest;
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
class BrandingTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private BrandingService brandingService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Prefeitura X", "pref-x", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void aplicaDefaultsQuandoNaoConfigurado() {
        BrandingResponse b = brandingService.obter();
        assertThat(b.nomeEnte()).isEqualTo("Prefeitura X");
        assertThat(b.nomeApp()).isEqualTo("Ponto Municipal");
        assertThat(b.corPrimaria()).isEqualTo("#1351B4");
        assertThat(b.corAcento()).isEqualTo("#1F6E5C");
    }

    @Test
    void definirPersonalizaPorEnte() {
        brandingService.definir(new DefinirBrandingRequest(
                "Ponto Goiânia", "https://cdn.exemplo/logo.png", "#0A4DA6", "#C2410C"));

        BrandingResponse b = brandingService.obter();
        assertThat(b.nomeApp()).isEqualTo("Ponto Goiânia");
        assertThat(b.logoUrl()).isEqualTo("https://cdn.exemplo/logo.png");
        assertThat(b.corPrimaria()).isEqualTo("#0A4DA6");
        assertThat(b.corAcento()).isEqualTo("#C2410C");
    }
}
