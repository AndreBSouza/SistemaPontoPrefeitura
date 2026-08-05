package br.gov.ponto.conformidade;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.auditoria.domain.AuditoriaEvento;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.CompetenciaService;
import br.gov.ponto.jornada.EscalaService;
import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.lgpd.LgpdService;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.relatorios.AfdService;
import br.gov.ponto.relatorios.RelatorioService;
import br.gov.ponto.relatorios.api.AfdResponse;
import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
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
class ConformidadeTest {

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
    private CompetenciaService competenciaService;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private RelatorioService relatorioService;
    @Autowired
    private AfdService afdService;
    @Autowired
    private br.gov.ponto.relatorios.PdfEspelhoService pdfEspelhoService;
    @Autowired
    private br.gov.ponto.relatorios.IndicadoresService indicadoresService;
    @Autowired
    private br.gov.ponto.tenant.LogoService logoService;
    @Autowired
    private br.gov.ponto.tenant.BrandingService brandingService;
    @Autowired
    private br.gov.ponto.relatorios.DossieService dossieService;
    @Autowired
    private LgpdService lgpdService;
    @Autowired
    private RegistroPontoRepository registroRepository;

    private UUID tenantId;
    private UUID servidorId;
    private UUID vinculoId;
    private final AtomicLong nsr = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente C", "ente-c", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        brandingService.definirCnpj("11222333000181"); // AFD exige CNPJ do ente

        var servidor = servidorService.criar(new CriarServidorRequest(
                "12345678901", "Joana", "joana@ente.gov.br",
                List.of(new CriarVinculoRequest("M-8", Regime.ESTATUTARIO, "Analista", 40))));
        servidorId = servidor.id();
        vinculoId = servidor.vinculos().get(0).id();

        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornadaId, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(vinculoId, jornadaId, DATA.minusMonths(1), null));

        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 13, 0); // hora extra de 60 min
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
    void auditoriaRegistraFechamento() {
        competenciaService.fechar(vinculoId, COMPETENCIA);
        assertThat(auditoriaService.listar())
                .extracting(AuditoriaEvento::getAcao)
                .contains("FECHAMENTO");
    }

    @Test
    void relatorioAfdEConformidade() {
        RelatorioFrequenciaResponse rel = relatorioService.frequenciaMensal(vinculoId, COMPETENCIA);
        assertThat(rel.minutosHoraExtra()).isEqualTo(60);

        AfdResponse afd = afdService.gerar(COMPETENCIA);
        assertThat(afd.totalRegistros()).isEqualTo(2);
        assertThat(afd.hashSha256()).isNotBlank();
        assertThat(afd.conteudo()).contains("12345678901");

        // Layout largura-fixa: cabeçalho (tipo 1), 2 marcações (tipo 3) e trailer (tipo 9).
        String[] linhas = afd.conteudo().split("\n");
        assertThat(linhas[0]).startsWith("0000000001");           // NSR(9 zeros) + tipo "1"
        assertThat(linhas[linhas.length - 1]).startsWith("9999999999"); // NSR(9 noves) + tipo "9"
        assertThat(java.util.Arrays.stream(linhas)
                .filter(l -> l.length() > 9 && l.charAt(9) == '3').count()).isEqualTo(2);
        // Empregado (tipo 5): 1 registro (a servidora Joana), com o CPF no conteúdo.
        assertThat(java.util.Arrays.stream(linhas)
                .filter(l -> l.length() > 9 && l.charAt(9) == '5').count()).isEqualTo(1);

        AfdResponse aej = afdService.gerarAej(COMPETENCIA);
        assertThat(aej.totalRegistros()).isGreaterThanOrEqualTo(1);
        assertThat(aej.hashSha256()).isNotBlank();

        var conf = relatorioService.conformidadeIn008(COMPETENCIA);
        assertThat(conf.totalServidores()).isEqualTo(1);
        assertThat(conf.totalRegistros()).isEqualTo(2);
    }

    @Test
    void afdExigeCnpjDoEnte() {
        UUID semCnpj = tenantService.criar(
                new CriarTenantRequest("Sem CNPJ", "ente-sem-cnpj", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(semCnpj.toString());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> afdService.gerar(COMPETENCIA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    void espelhoEmPdf() {
        byte[] pdf = pdfEspelhoService.gerarPdf(vinculoId, COMPETENCIA);
        assertThat(pdf).isNotEmpty();
        // Assinatura de arquivo PDF: começa com "%PDF-".
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void declaracaoDeFrequenciaEmPdf() {
        byte[] pdf = pdfEspelhoService.gerarDeclaracao(vinculoId, COMPETENCIA);
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void indicadoresDeGestao() {
        var ind = indicadoresService.obter(COMPETENCIA);
        assertThat(ind.totalVinculos()).isGreaterThanOrEqualTo(1);
        assertThat(ind.registrosNoPeriodo()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void espelhoComLogoDoEnteGeraPdf() {
        // PNG 1x1 válido — exercita o embed do logo do ente no cabeçalho do PDF.
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        logoService.salvar(png, "image/png");

        byte[] pdf = pdfEspelhoService.gerarPdf(vinculoId, COMPETENCIA);
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void dossieDeConformidadeEmpacotaAsEvidencias() {
        var dossie = dossieService.gerar(COMPETENCIA);
        assertThat(dossie.conformidade().totalServidores()).isEqualTo(1);
        assertThat(dossie.afdHashSha256()).isNotBlank();
        assertThat(dossie.afdTotalRegistros()).isEqualTo(2);
        assertThat(dossie.indicadores().totalVinculos()).isGreaterThanOrEqualTo(1);
        assertThat(dossie.prazoSubmissao()).isEqualTo(COMPETENCIA.plusMonths(1).atDay(15));
        assertThat(dossie.escudos()).isNotEmpty();
    }

    @Test
    void lgpdConsentimentoExportacaoEEliminacao() {
        lgpdService.registrarConsentimento(servidorId, "BIOMETRIA", true);
        assertThat(lgpdService.consentimentoVigente(servidorId, "BIOMETRIA")).isTrue();

        assertThat(lgpdService.exportarDadosTitular(servidorId).nome()).isEqualTo("Joana");

        var elim = lgpdService.eliminarDadosTitular(servidorId);
        assertThat(elim.status()).isEqualTo("ANONIMIZADO");
        assertThat(lgpdService.exportarDadosTitular(servidorId).nome()).isEqualTo("[REMOVIDO LGPD]");
    }
}
