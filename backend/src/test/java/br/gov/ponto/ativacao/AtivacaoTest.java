package br.gov.ponto.ativacao;

import br.gov.ponto.ativacao.api.AtivacaoResponse;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class AtivacaoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private AtivacaoService ativacaoService;

    private UUID tenantId;
    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente AT", "ente-at", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var servidor = servidorService.criar(new CriarServidorRequest(
                "55555555555", "Marta", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Auxiliar", 40))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void gerarEAtivarEmiteTokenEResolveTenant() {
        GerarCodigoResponse codigo = ativacaoService.gerar(vinculoId, null);
        assertThat(codigo.codigo()).isNotBlank();

        // O app ativa sem conhecer o tenant — o codigo resolve o ente.
        TenantContext.clear();
        AtivacaoResponse ativacao = ativacaoService.ativar(codigo.codigo(), "Galaxy A10");
        assertThat(ativacao.deviceToken()).isNotBlank();
        assertThat(ativacao.tenantId()).isEqualTo(tenantId);
        assertThat(ativacao.vinculoId()).isEqualTo(vinculoId);

        // Autenticacao por token (tambem sem tenant no contexto).
        var dispositivo = ativacaoService.autenticarPorToken(ativacao.deviceToken());
        assertThat(dispositivo).isPresent();
        assertThat(dispositivo.get().tenantId()).isEqualTo(tenantId);

        TenantContext.set(tenantId.toString());
        assertThat(ativacaoService.listarDispositivos(vinculoId)).hasSize(1);
    }

    @Test
    void codigoNaoPodeSerUsadoDuasVezes() {
        GerarCodigoResponse codigo = ativacaoService.gerar(vinculoId, null);
        ativacaoService.ativar(codigo.codigo(), "Aparelho 1");

        assertThatThrownBy(() -> ativacaoService.ativar(codigo.codigo(), "Aparelho 2"))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void revogarBloqueiaAutenticacao() {
        GerarCodigoResponse codigo = ativacaoService.gerar(vinculoId, null);
        AtivacaoResponse ativacao = ativacaoService.ativar(codigo.codigo(), "Aparelho");

        // popula o cache de autenticação...
        assertThat(ativacaoService.autenticarPorToken(ativacao.deviceToken())).isPresent();

        ativacaoService.revogar(ativacao.dispositivoId());

        // ...e a revogação invalida o cache → próxima autenticação falha
        assertThat(ativacaoService.autenticarPorToken(ativacao.deviceToken())).isEmpty();
    }
}
