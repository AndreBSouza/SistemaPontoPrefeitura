package br.gov.ponto.me;

import br.gov.ponto.apuracao.JustificativaService;
import br.gov.ponto.apuracao.api.JustificativaResponse;
import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.apuracao.domain.TipoJustificativa;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.AcessoNegadoException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class GestorTest {

    private static final LocalDate DATA = LocalDate.of(2026, 3, 2);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private JustificativaService justificativaService;
    @Autowired
    private GestorService gestorService;

    private UUID gestorVinculoId;
    private UUID subVinculoId;
    private UUID outroVinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente G", "ente-g", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());

        var gestor = servidorService.criar(new CriarServidorRequest(
                "11111111111", "Gestor", null,
                List.of(new CriarVinculoRequest("M-G", Regime.ESTATUTARIO, "Chefe", 40))));
        gestorVinculoId = gestor.vinculos().get(0).id();

        // Órgão A com o gestor como chefia; subordinado lotado em A.
        UUID orgaoA = lotacaoService.criar("Secretaria A", "SEA").getId();
        lotacaoService.definirChefia(orgaoA, gestor.id());
        var sub = servidorService.criar(new CriarServidorRequest(
                "22222222222", "Subordinado", null,
                List.of(new CriarVinculoRequest("M-S", Regime.ESTATUTARIO, "Servidor", 40))));
        subVinculoId = sub.vinculos().get(0).id();
        servidorService.lotarVinculo(subVinculoId, orgaoA);

        // Órgão B (de fora do time do gestor).
        UUID orgaoB = lotacaoService.criar("Secretaria B", "SEB").getId();
        var outro = servidorService.criar(new CriarServidorRequest(
                "33333333333", "Outro", null,
                List.of(new CriarVinculoRequest("M-O", Regime.ESTATUTARIO, "Servidor", 40))));
        outroVinculoId = outro.vinculos().get(0).id();
        servidorService.lotarVinculo(outroVinculoId, orgaoB);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private UUID justificativaDe(UUID vinculoId, LocalDate inicio, LocalDate fim) {
        return justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, inicio, fim, "motivo", null)).id();
    }

    @Test
    void souGestorApenasParaQuemChefia() {
        assertThat(gestorService.souGestor(gestorVinculoId)).isTrue();
        assertThat(gestorService.souGestor(subVinculoId)).isFalse();
    }

    @Test
    void pendentesDoTimeNaoIncluemServidoresDeForaDaChefia() {
        justificativaDe(subVinculoId, DATA, DATA);
        justificativaDe(outroVinculoId, DATA, DATA);

        List<JustificativaResponse> pendentes = gestorService.pendentesDoMeuTime(gestorVinculoId);
        assertThat(pendentes).extracting(JustificativaResponse::vinculoId).containsExactly(subVinculoId);
    }

    @Test
    void aprovaJustificativaDoProprioTime() {
        UUID jId = justificativaDe(subVinculoId, DATA, DATA);
        var aprovada = gestorService.aprovar(gestorVinculoId, jId, "deferido");
        assertThat(aprovada.status()).isEqualTo(StatusJustificativa.APROVADA);
    }

    @Test
    void naoAprovaJustificativaDeForaDoTime() {
        UUID jId = justificativaDe(outroVinculoId, DATA, DATA);
        assertThatThrownBy(() -> gestorService.aprovar(gestorVinculoId, jId, "x"))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void respeitaAlcadaDoGestorEmAusenciaLonga() {
        UUID jId = justificativaDe(subVinculoId, DATA, DATA.plusDays(9)); // 10 dias > alçada
        assertThatThrownBy(() -> gestorService.aprovar(gestorVinculoId, jId, "x"))
                .isInstanceOf(ConflitoException.class);
    }
}
