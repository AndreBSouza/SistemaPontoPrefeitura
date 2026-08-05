package br.gov.ponto.relatorios;

import br.gov.ponto.apuracao.JustificativaService;
import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.apuracao.domain.TipoJustificativa;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.relatorios.api.AbonoResponse;
import br.gov.ponto.relatorios.api.InconsistenciasResponse;
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
class RelatoriosControleTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATA = LocalDate.of(2026, 3, 2);
    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private RegistroPontoRepository registroRepository;
    @Autowired
    private InconsistenciaService inconsistenciaService;
    @Autowired
    private JustificativaService justificativaService;
    @Autowired
    private AbonoRelatorioService abonoRelatorioService;

    private UUID tenantId;
    private UUID vinculoId;
    private final AtomicLong nsr = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Ctrl", "ente-ctrl", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var s = servidorService.criar(new CriarServidorRequest("70707070707", "Téo", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));
        vinculoId = s.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void registrar(TipoMarcacao tipo, int hora, int minuto) {
        Instant instante = DATA.atTime(hora, minuto).atZone(ZONE).toInstant();
        registroRepository.save(new RegistroPonto(tenantId, vinculoId, nsr.getAndIncrement(),
                tipo, OrigemRegistro.MOBILE, instante, instante, null, null, false,
                UUID.randomUUID().toString()));
    }

    @Test
    void inconsistenciaApontaDiaComMarcacaoImpar() {
        // 3 marcações no dia (ímpar) => intervalo aberto, possível esquecimento.
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.INTERVALO_INICIO, 12, 0);
        registrar(TipoMarcacao.INTERVALO_FIM, 13, 0);

        InconsistenciasResponse r = inconsistenciaService.detectar(COMPETENCIA);
        assertThat(r.inconsistencias()).hasSize(1);
        assertThat(r.inconsistencias().get(0).data()).isEqualTo(DATA);
        assertThat(r.inconsistencias().get(0).marcacoes()).isEqualTo(3);
    }

    @Test
    void diaComMarcacaoParNaoEhInconsistente() {
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 12, 0);
        assertThat(inconsistenciaService.detectar(COMPETENCIA).inconsistencias()).isEmpty();
    }

    @Test
    void relatorioDeAbonosTrazAJustificativaComDecisao() {
        var j = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA, DATA, "consulta", null));
        justificativaService.aprovar(j.id(), "deferido");

        List<AbonoResponse> abonos = abonoRelatorioService.abonos(COMPETENCIA);
        assertThat(abonos).hasSize(1);
        assertThat(abonos.get(0).status()).isEqualTo(StatusJustificativa.APROVADA);
        assertThat(abonos.get(0).motivoDecisao()).isEqualTo("deferido");
        assertThat(abonos.get(0).servidor()).isEqualTo("Téo");

        String csv = abonoRelatorioService.exportarCsv(COMPETENCIA);
        assertThat(csv).startsWith("servidor;tipo;dataInicio;dataFim;status;motivoDecisao");
        assertThat(csv).contains("ATESTADO").contains("APROVADA");
    }
}
