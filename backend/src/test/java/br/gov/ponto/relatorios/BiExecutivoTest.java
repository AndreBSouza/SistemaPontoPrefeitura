package br.gov.ponto.relatorios;

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
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.relatorios.api.BiExecutivoResponse;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class BiExecutivoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATA = LocalDate.of(2026, 3, 2);
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
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private RegistroPontoRepository registroRepository;
    @Autowired
    private BiExecutivoService biExecutivoService;
    @Autowired
    private RoiService roiService;

    private UUID tenantId;
    private final AtomicLong nsr = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Bi", "ente-bi", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void agregaHoraExtraEFantasmaPorOrgao() {
        UUID orgao = lotacaoService.criar("Secretaria de Saúde", "SMS").getId();

        // Vínculo A: bate ponto com 1h extra.
        var srvA = servidorService.criar(new CriarServidorRequest("11111111111", "Ari", null,
                List.of(new CriarVinculoRequest("A-1", Regime.ESTATUTARIO, "Agente", 40))));
        UUID vA = srvA.vinculos().get(0).id();
        UUID jornada = jornadaService.criar(new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornada, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(vA, jornada, DATA.minusMonths(1), null));
        lotar(vA, orgao);
        registrar(vA, TipoMarcacao.ENTRADA, 8, 0);
        registrar(vA, TipoMarcacao.SAIDA, 13, 0); // +60 min hora extra

        // Vínculo B: nenhuma batida => "fantasma".
        var srvB = servidorService.criar(new CriarServidorRequest("22222222222", "Bea", null,
                List.of(new CriarVinculoRequest("B-1", Regime.ESTATUTARIO, "Auxiliar", 40))));
        UUID vB = srvB.vinculos().get(0).id();
        lotar(vB, orgao);

        BiExecutivoResponse bi = biExecutivoService.porOrgao(COMPETENCIA);
        assertThat(bi.orgaos()).hasSize(1);
        BiExecutivoResponse.OrgaoBi linha = bi.orgaos().get(0);
        assertThat(linha.vinculos()).isEqualTo(2);
        assertThat(linha.horaExtraMinutos()).isEqualTo(60);
        assertThat(linha.fantasmas()).isEqualTo(1);
        assertThat(bi.totalHoraExtraMinutos()).isEqualTo(60);
        assertThat(bi.totalFantasmas()).isEqualTo(1);
    }

    @Test
    void roiEstimaEconomiaDeHoraExtra() {
        UUID orgao = lotacaoService.criar("Saúde", "SMS").getId();
        var srv = servidorService.criar(new CriarServidorRequest("33333333333", "Cau", null,
                List.of(new CriarVinculoRequest("R-1", Regime.ESTATUTARIO, "Agente", 40))));
        UUID v = srv.vinculos().get(0).id();
        UUID jornada = jornadaService.criar(new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornada, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(v, jornada, DATA.minusMonths(1), null));
        lotar(v, orgao);
        registrar(v, TipoMarcacao.ENTRADA, 8, 0);
        registrar(v, TipoMarcacao.SAIDA, 13, 0); // 60 min de hora extra

        // custoHora=30, reduzir 50% da hora extra; faltas ignoradas (custoDiaFalta=0).
        var roi = roiService.estimar(COMPETENCIA, new java.math.BigDecimal("30"),
                new java.math.BigDecimal("50"), java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO);
        assertThat(roi.custoHoraExtraAtualReais()).isEqualByComparingTo("30.00"); // 1h × R$30
        assertThat(roi.economiaHoraExtraReais()).isEqualByComparingTo("15.00");   // 50% de 30
        assertThat(roi.economiaTotalEstimadaReais()).isEqualByComparingTo("15.00");
    }

    private void lotar(UUID vinculoId, UUID orgaoId) {
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId).orElseThrow();
        v.setLotacaoId(orgaoId);
        vinculoRepository.save(v);
    }

    private void registrar(UUID vinculoId, TipoMarcacao tipo, int hora, int minuto) {
        Instant instante = DATA.atTime(hora, minuto).atZone(ZONE).toInstant();
        registroRepository.save(new RegistroPonto(tenantId, vinculoId, nsr.getAndIncrement(),
                tipo, OrigemRegistro.MOBILE, instante, instante, null, null, false,
                UUID.randomUUID().toString()));
    }
}
