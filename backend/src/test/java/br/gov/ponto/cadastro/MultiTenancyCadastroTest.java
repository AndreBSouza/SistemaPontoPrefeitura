package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica o isolamento multi-tenant por RLS e a unicidade de CPF por tenant,
 * usando PostgreSQL embarcado (sem Docker).
 */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class MultiTenancyCadastroTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ServidorService servidorService;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantA = tenantService.criar(
                new CriarTenantRequest("Prefeitura A", "prefeitura-a", TipoPoder.EXECUTIVO)).id();
        tenantB = tenantService.criar(
                new CriarTenantRequest("Prefeitura B", "prefeitura-b", TipoPoder.EXECUTIVO)).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void isolaServidoresPorTenant() {
        TenantContext.set(tenantA.toString());
        servidorService.criar(new CriarServidorRequest("11111111111", "Ana", "ana@a.gov.br", List.of()));

        TenantContext.set(tenantB.toString());
        servidorService.criar(new CriarServidorRequest("22222222222", "Bruno", "bruno@b.gov.br", List.of()));

        TenantContext.set(tenantA.toString());
        List<?> listaA = servidorService.listar();
        assertThat(listaA).hasSize(1);

        TenantContext.set(tenantB.toString());
        var listaB = servidorService.listar();
        assertThat(listaB).hasSize(1);
        assertThat(listaB.get(0).nome()).isEqualTo("Bruno");
    }

    @Test
    void cpfUnicoNoTenantMasReutilizavelEntreTenants() {
        TenantContext.set(tenantA.toString());
        servidorService.criar(new CriarServidorRequest("33333333333", "Carla", null, List.of()));

        // mesmo CPF, mesmo tenant -> conflito
        assertThatThrownBy(() -> servidorService.criar(
                new CriarServidorRequest("33333333333", "Carla II", null, List.of())))
                .isInstanceOf(ConflitoException.class);

        // mesmo CPF, outro tenant -> permitido
        TenantContext.set(tenantB.toString());
        assertThatCode(() -> servidorService.criar(
                new CriarServidorRequest("33333333333", "Carlos", null, List.of())))
                .doesNotThrowAnyException();
    }
}
