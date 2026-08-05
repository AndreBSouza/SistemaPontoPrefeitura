package br.gov.ponto.tenant;

import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.api.EntePublicoResponse;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class SubdominioTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private BrandingService brandingService;

    private UUID tenantA;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantA = tenantService.criar(new CriarTenantRequest("Ente A", "ente-a", TipoPoder.EXECUTIVO)).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void defineSubdominioEResolvePublicamente() {
        TenantContext.set(tenantA.toString());
        brandingService.definirSubdominio("cidade-a");
        TenantContext.clear(); // simula acesso público (app/login)

        EntePublicoResponse r = brandingService.resolverPorSubdominio("cidade-a");
        assertThat(r.slug()).isEqualTo("ente-a");
        assertThat(r.nomeApp()).isNotBlank();
        assertThat(r.corPrimaria()).isNotBlank();
    }

    @Test
    void subdominioEhUnicoEntreEntes() {
        TenantContext.set(tenantA.toString());
        brandingService.definirSubdominio("dup");
        TenantContext.clear();

        UUID tenantB = tenantService.criar(new CriarTenantRequest("Ente B", "ente-b", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantB.toString());
        assertThatThrownBy(() -> brandingService.definirSubdominio("dup"))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void rejeitaFormatoInvalido() {
        TenantContext.set(tenantA.toString());
        assertThatThrownBy(() -> brandingService.definirSubdominio("Cidade A!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolverSubdominioInexistenteFalha() {
        assertThatThrownBy(() -> brandingService.resolverPorSubdominio("naoexiste"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
