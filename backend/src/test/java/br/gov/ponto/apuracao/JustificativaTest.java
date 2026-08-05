package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.Ocorrencia;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.apuracao.domain.TipoJustificativa;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaService;
import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.domain.TipoJornada;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class JustificativaTest {

    private static final LocalDate DATA = LocalDate.of(2026, 3, 2);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private JornadaService jornadaService;
    @Autowired
    private EscalaService escalaService;
    @Autowired
    private JustificativaService justificativaService;
    @Autowired
    private ApuracaoService apuracaoService;

    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente W", "ente-w", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "77777777777", "Gabi", null,
                List.of(new CriarVinculoRequest("M-3", Regime.ESTATUTARIO, "Auxiliar", 40))));
        vinculoId = servidor.vinculos().get(0).id();

        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornadaId, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(vinculoId, jornadaId, DATA.minusMonths(1), null));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void workflowDeAprovacao() {
        var solicitada = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA, DATA, "consulta medica", null));
        assertThat(solicitada.status()).isEqualTo(StatusJustificativa.PENDENTE);
        assertThat(justificativaService.listarPendentes()).hasSize(1);

        var aprovada = justificativaService.aprovar(solicitada.id(), "deferido");
        assertThat(aprovada.status()).isEqualTo(StatusJustificativa.APROVADA);
        assertThat(justificativaService.listarPendentes()).isEmpty();
    }

    @Test
    void aprovacaoEmLoteDecideVariasEIgnoraJaDecididas() {
        var j1 = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA, DATA, "j1", null));
        var j2 = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA.plusDays(1), DATA.plusDays(1), "j2", null));
        var j3 = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA.plusDays(2), DATA.plusDays(2), "j3", null));
        // j3 já decidida individualmente antes do lote.
        justificativaService.rejeitar(j3.id(), "fora do prazo");

        var decididas = justificativaService.aprovarEmLote(
                List.of(j1.id(), j2.id(), j3.id(), UUID.randomUUID()), "deferido em lote");
        // Só j1 e j2 (pendentes) são processadas; j3 (decidida) e id inexistente são ignorados.
        assertThat(decididas).extracting(r -> r.status())
                .containsOnly(StatusJustificativa.APROVADA);
        assertThat(decididas).hasSize(2);
        assertThat(justificativaService.listarPendentes()).isEmpty();
    }

    @Test
    void alcadaLimitaGestorEmAusenciaLonga() {
        // Ausência de 10 dias (> alçada do gestor): gestor não aprova; RH/controle aprova.
        var longa = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA, DATA.plusDays(9), "afastamento longo", null));

        assertThatThrownBy(() -> justificativaService.aprovarComAlcada(longa.id(), "ok", false))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("alçada");

        var aprovada = justificativaService.aprovarComAlcada(longa.id(), "deferido pelo RH", true);
        assertThat(aprovada.status()).isEqualTo(StatusJustificativa.APROVADA);

        // Dentro da alçada (3 dias) o próprio gestor aprova.
        var curta = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA.plusDays(20), DATA.plusDays(22), "atestado", null));
        var ok = justificativaService.aprovarComAlcada(curta.id(), "deferido pelo gestor", false);
        assertThat(ok.status()).isEqualTo(StatusJustificativa.APROVADA);
    }

    @Test
    void justificativaAprovadaNeutralizaFalta() {
        var antes = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(antes.justificado()).isFalse();
        assertThat(antes.ocorrencias()).extracting(Ocorrencia::tipo).contains(TipoOcorrencia.FALTA);

        var j = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA, DATA, "atestado", null));
        justificativaService.aprovar(j.id(), null);

        var depois = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(depois.justificado()).isTrue();
        assertThat(depois.ocorrencias()).extracting(Ocorrencia::tipo)
                .doesNotContain(TipoOcorrencia.FALTA);
    }
}
