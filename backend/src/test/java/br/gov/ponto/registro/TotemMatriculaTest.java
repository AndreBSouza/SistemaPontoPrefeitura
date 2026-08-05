package br.gov.ponto.registro;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.BrandingResponse;
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
class TotemMatriculaTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private TotemService totemService;
    @Autowired
    private AtivacaoService ativacaoService;
    @Autowired
    private RegistroPontoRepository registroRepository;

    private UUID tenantId;
    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Prefeitura Totem", "pref-totem", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var s = servidorService.criar(new CriarServidorRequest("16161616161", "Zé", null,
                List.of(new CriarVinculoRequest("MAT-99", Regime.ESTATUTARIO, "Gari", 40))));
        vinculoId = s.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void totemBatePorMatricula() {
        var resp = totemService.baterPorMatricula("MAT-99");
        assertThat(resp.nsr()).isPositive();

        var registros = registroRepository.findByVinculoIdAndTenantIdOrderByNsr(vinculoId, tenantId);
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).getOrigem()).isEqualTo(OrigemRegistro.TOTEM);
    }

    @Test
    void matriculaInexistenteNoTotemFalha() {
        assertThatThrownBy(() -> totemService.baterPorMatricula("NAO-EXISTE"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void brandingDaTelaDeAtivacaoResolveOEntePeloCodigo() {
        GerarCodigoResponse codigo = ativacaoService.gerar(vinculoId, null);
        BrandingResponse branding = ativacaoService.brandingDoCodigo(codigo.codigo());
        assertThat(branding.nomeEnte()).isEqualTo("Prefeitura Totem");
        assertThat(branding.corPrimaria()).isNotBlank();
    }
}
