package br.gov.ponto.biometria;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.lgpd.LgpdService;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class BiometriaTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LgpdService lgpdService;
    @Autowired
    private BiometriaService biometriaService;

    private UUID servidorId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente B2", "ente-b2", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        servidorId = servidorService.criar(new CriarServidorRequest(
                "33333333333", "Lia", null, List.of())).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void exigeConsentimentoParaCadastrarReferencia() {
        assertThatThrownBy(() -> biometriaService.cadastrarReferencia(servidorId, "template-hash-abc"))
                .isInstanceOf(ConflitoException.class);

        lgpdService.registrarConsentimento(servidorId, "BIOMETRIA", true);

        assertThatCode(() -> biometriaService.cadastrarReferencia(servidorId, "template-hash-abc"))
                .doesNotThrowAnyException();
    }
}
