package br.gov.ponto.ausencia;

import br.gov.ponto.apuracao.ApuracaoService;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.ausencia.api.CoberturaResponse;
import br.gov.ponto.ausencia.domain.TipoAusencia;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaService;
import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.me.MeService;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class AusenciaTest {

    private static final LocalDate DATA = LocalDate.of(2026, 3, 2); // segunda-feira
    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private JornadaService jornadaService;
    @Autowired
    private EscalaService escalaService;
    @Autowired
    private ApuracaoService apuracaoService;
    @Autowired
    private AusenciaService ausenciaService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private MeService meService;

    private UUID tenantId;
    private UUID v1;
    private UUID v2;
    private UUID orgaoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Aus", "ente-aus", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "20202020202", "Nina", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40),
                        new CriarVinculoRequest("M-2", Regime.ESTATUTARIO, "Auxiliar", 40))));
        v1 = servidor.vinculos().get(0).id();
        v2 = servidor.vinculos().get(1).id();

        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornadaId, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(v1, jornadaId, DATA.minusMonths(1), null));
        escalaService.atribuir(new CriarEscalaRequest(v2, jornadaId, DATA.minusMonths(1), null));

        orgaoId = lotacaoService.criar("Secretaria", "SEC").getId();
        for (UUID vid : List.of(v1, v2)) {
            Vinculo v = vinculoRepository.findByIdAndTenantId(vid, tenantId).orElseThrow();
            v.setLotacaoId(orgaoId);
            vinculoRepository.save(v);
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void feriasNeutralizaFaltaETornaDiaNaoUtil() {
        assertThat(apuracaoService.apurarDia(v1, DATA).ocorrencias())
                .anyMatch(o -> o.tipo() == TipoOcorrencia.FALTA);

        ausenciaService.agendar(v1, TipoAusencia.FERIAS, DATA, DATA.plusDays(10), "Férias regulamentares");

        var ap = apuracaoService.apurarDia(v1, DATA);
        assertThat(ap.diaUtil()).isFalse();
        assertThat(ap.minutosEsperados()).isZero();
        assertThat(ap.ocorrencias()).noneMatch(o -> o.tipo() == TipoOcorrencia.FALTA);
    }

    @Test
    void viagemAServicoTambemNeutralizaFalta() {
        // Diárias/deslocamentos (12.4.6): viagem a serviço é ausência que não gera falta.
        ausenciaService.agendar(v1, TipoAusencia.VIAGEM, DATA, DATA.plusDays(2), "Capacitação na capital");
        assertThat(apuracaoService.apurarDia(v1, DATA).ocorrencias())
                .noneMatch(o -> o.tipo() == TipoOcorrencia.FALTA);
    }

    @Test
    void ausenciaSoAfetaOProprioVinculo() {
        ausenciaService.agendar(v1, TipoAusencia.LICENCA_MEDICA, DATA, DATA.plusDays(3), null);
        // v2 não está ausente: segue gerando FALTA sem batidas.
        assertThat(apuracaoService.apurarDia(v2, DATA).ocorrencias())
                .anyMatch(o -> o.tipo() == TipoOcorrencia.FALTA);
    }

    @Test
    void servidorVeAsPropriasAusenciasNoApp() {
        ausenciaService.agendar(v1, TipoAusencia.FERIAS, DATA, DATA.plusDays(9), null);
        var minhas = meService.minhasAusencias(v1);
        assertThat(minhas).hasSize(1);
        assertThat(minhas.get(0).tipo()).isEqualTo(TipoAusencia.FERIAS);
        assertThat(minhas.get(0).dias()).isEqualTo(10);
        // O outro vínculo não vê a ausência alheia.
        assertThat(meService.minhasAusencias(v2)).isEmpty();
    }

    @Test
    void coberturaMostraQuemEstaAusenteNoOrgao() {
        ausenciaService.agendar(v1, TipoAusencia.FERIAS, DATA, DATA.plusDays(5), null);

        CoberturaResponse cobertura = ausenciaService.cobertura(orgaoId, COMPETENCIA);
        assertThat(cobertura.totalVinculos()).isEqualTo(2);
        assertThat(cobertura.ausencias()).hasSize(1);
        assertThat(cobertura.ausencias().get(0).vinculoId()).isEqualTo(v1);
        assertThat(cobertura.ausencias().get(0).tipo()).isEqualTo(TipoAusencia.FERIAS);
    }
}
