package br.gov.ponto.tenant;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TenantLogo;
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
class LogoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private LogoService logoService;
    @Autowired
    private BrandingService brandingService;

    private String slug;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var tenant = tenantService.criar(new CriarTenantRequest("Ente L", "ente-l", TipoPoder.EXECUTIVO));
        slug = tenant.slug();
        TenantContext.set(tenant.id().toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void salvaLogoNoBancoEServePorSlugAtualizandoOBranding() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        logoService.salvar(png, "image/png");

        // O branding passa a apontar para o endpoint público de serviço.
        assertThat(brandingService.obter().logoUrl()).isEqualTo("/api/publico/branding/" + slug + "/logo");

        // Serve por slug (simula acesso público, sem contexto de tenant).
        TenantContext.clear();
        TenantLogo logo = logoService.buscarPorSlug(slug);
        assertThat(logo.getContentType()).isEqualTo("image/png");
        assertThat(logo.getConteudo()).isEqualTo(png);
    }

    @Test
    void substituiOLogoExistente() {
        logoService.salvar(new byte[]{1}, "image/png");
        logoService.salvar(new byte[]{2, 3}, "image/jpeg");
        TenantLogo logo = logoService.buscarPorSlug(slug);
        assertThat(logo.getContentType()).isEqualTo("image/jpeg");
        assertThat(logo.getConteudo()).hasSize(2);
    }

    @Test
    void rejeitaArquivoQueNaoEImagem() {
        assertThatThrownBy(() -> logoService.salvar(new byte[]{1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
