package br.gov.ponto.espelho;

import br.gov.ponto.apuracao.JustificativaService;
import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.TipoJustificativa;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.api.PendenciaFechamentoResponse;
import br.gov.ponto.espelho.domain.StatusCompetencia;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class EspelhoFechamentoTest {

    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private EspelhoService espelhoService;
    @Autowired
    private CompetenciaService competenciaService;
    @Autowired
    private JustificativaService justificativaService;
    @Autowired
    private PendenciaFechamentoService pendenciaFechamentoService;

    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente E", "ente-e", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        var servidor = servidorService.criar(new CriarServidorRequest(
                "99999999999", "Igor", null,
                List.of(new CriarVinculoRequest("M-2", Regime.ESTATUTARIO, "Assistente", 40))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void geraEspelhoEFechaComBloqueioDeRefechamento() {
        var espelho = espelhoService.gerar(vinculoId, COMPETENCIA);
        assertThat(espelho.dias()).hasSize(31);
        assertThat(espelho.status()).isEqualTo("ABERTA");

        var fechada = competenciaService.fechar(vinculoId, COMPETENCIA);
        assertThat(fechada.getStatus()).isEqualTo(StatusCompetencia.FECHADA);

        assertThatThrownBy(() -> competenciaService.fechar(vinculoId, COMPETENCIA))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void cienciaEReabertura() {
        var comCiencia = competenciaService.darCiencia(vinculoId, COMPETENCIA, "assinatura-eletronica");
        assertThat(comCiencia.getCienciaEm()).isNotNull();

        competenciaService.fechar(vinculoId, COMPETENCIA);
        var reaberta = competenciaService.reabrir(vinculoId, COMPETENCIA, "correcao de marcacao");
        assertThat(reaberta.getStatus()).isEqualTo(StatusCompetencia.ABERTA);
    }

    @Test
    void fechamentoEmLoteEsvaziaOPainelDePendencias() {
        // Segundo vínculo no mesmo ente, para um lote significativo.
        var bia = servidorService.criar(new CriarServidorRequest(
                "88888888888", "Bia", null,
                List.of(new CriarVinculoRequest("M-3", Regime.ESTATUTARIO, "Auxiliar", 40))));
        UUID vinculoBia = bia.vinculos().get(0).id();

        // Antes de fechar: ambos pendentes.
        PendenciaFechamentoResponse antes = pendenciaFechamentoService.pendentes(COMPETENCIA);
        assertThat(antes.totalVinculos()).isEqualTo(2);
        assertThat(antes.pendentes()).isEqualTo(2);
        assertThat(antes.fechadas()).isZero();

        // Fecha os dois de uma vez.
        assertThat(competenciaService.fecharEmLote(List.of(vinculoId, vinculoBia), COMPETENCIA)).hasSize(2);

        PendenciaFechamentoResponse depois = pendenciaFechamentoService.pendentes(COMPETENCIA);
        assertThat(depois.fechadas()).isEqualTo(2);
        assertThat(depois.pendentes()).isZero();
        assertThat(depois.orgaos()).isEmpty();

        // Idempotente: refechar não duplica (já fechadas são puladas).
        assertThat(competenciaService.fecharEmLote(List.of(vinculoId, vinculoBia), COMPETENCIA)).isEmpty();
    }

    @Test
    void competenciaFechadaBloqueiaJustificativa() {
        competenciaService.fechar(vinculoId, COMPETENCIA);

        assertThatThrownBy(() -> justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10), "atestado", null)))
                .isInstanceOf(ConflitoException.class);
    }
}
