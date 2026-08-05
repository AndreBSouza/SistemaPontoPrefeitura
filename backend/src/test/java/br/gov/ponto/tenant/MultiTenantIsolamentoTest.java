package br.gov.ponto.tenant;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.registro.domain.OrigemRegistro;
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

/**
 * Isolamento entre entes (multi-tenant). Cobre tanto as tabelas com RLS (registro_ponto)
 * quanto as sem RLS, protegidas por escopo de consulta (dispositivo/codigo_ativacao):
 * o ente B nunca enxerga dados do ente A nem opera sobre vínculos de A.
 */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class MultiTenantIsolamentoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private RegistroService registroService;
    @Autowired
    private AtivacaoService ativacaoService;

    private UUID tenantA;
    private UUID tenantB;
    private UUID vinculoA;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantA = tenantService.criar(new CriarTenantRequest("Ente A", "ente-a", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantA.toString());
        vinculoA = servidorService.criar(new CriarServidorRequest(
                "11111111111", "Ana", null,
                List.of(new CriarVinculoRequest("A-1", Regime.ESTATUTARIO, "Cargo", 40))))
                .vinculos().get(0).id();

        TenantContext.clear();
        tenantB = tenantService.criar(new CriarTenantRequest("Ente B", "ente-b", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantB.toString());
        servidorService.criar(new CriarServidorRequest(
                "22222222222", "Bruno", null,
                List.of(new CriarVinculoRequest("B-1", Regime.ESTATUTARIO, "Cargo", 40))));
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registrosNaoVazamEntreEntes() {
        TenantContext.set(tenantA.toString());
        registroService.bater(new BaterPontoRequest(vinculoA, OrigemRegistro.MOBILE, null, null, null, false, "iso-a"));
        assertThat(registroService.listarPorVinculo(vinculoA)).hasSize(1);
        TenantContext.clear();

        TenantContext.set(tenantB.toString());
        assertThat(registroService.listarPorVinculo(vinculoA)).isEmpty();
    }

    @Test
    void dispositivosECodigosNaoVazamEntreEntes() {
        TenantContext.set(tenantA.toString());
        GerarCodigoResponse codigo = ativacaoService.gerar(vinculoA, null);
        ativacaoService.ativar(codigo.codigo(), "Aparelho A");
        assertThat(ativacaoService.listarDispositivos(vinculoA)).hasSize(1);
        TenantContext.clear();

        TenantContext.set(tenantB.toString());
        // B não enxerga dispositivos de A...
        assertThat(ativacaoService.listarDispositivos(vinculoA)).isEmpty();
        // ...nem pode gerar código para um vínculo que não é seu.
        assertThatThrownBy(() -> ativacaoService.gerar(vinculoA, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
