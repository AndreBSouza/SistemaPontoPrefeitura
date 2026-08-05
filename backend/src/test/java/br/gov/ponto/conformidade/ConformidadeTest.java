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

// O REP-P precisa se identificar no AFD/AEJ (art. 91); sem 'rep.inpi' a emissão é recusada.
@SpringBootTest(properties = {
        "rep.inpi=51202300012345",
        "rep.desenvolvedor.cnpj=99888777000166",
        "rep.desenvolvedor.nome=Fornecedor de Software LTDA",
        "rep.desenvolvedor.email=suporte@fornecedor.com.br"
})
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
    private br.gov.ponto.relatorios.AejService aejService;
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

        // Leiaute vigente (v004): linhas em CRLF, cabeçalho tipo 1 com 302 caracteres.
        String[] linhas = afd.conteudo().split("\r\n");
        assertThat(linhas[0]).startsWith("0000000001").hasSize(302);
        // As duas marcações vão no tipo "7" (REP-P) — o tipo "3" é de REP-C/REP-A.
        assertThat(java.util.Arrays.stream(linhas)
                .filter(l -> l.length() > 9 && l.charAt(9) == '7').count()).isEqualTo(2);
        assertThat(java.util.Arrays.stream(linhas)
                .filter(l -> l.length() > 9 && l.charAt(9) == '3').count())
                .as("REP-P nao emite marcacao no tipo 3").isZero();
        // O AFD cobre um período: a inclusão da servidora aconteceu HOJE, não na competência
        // apurada, então não entra neste arquivo (ela estará no AFD do mês da inclusão).
        assertThat(java.util.Arrays.stream(linhas)
                .filter(l -> l.length() > 9 && l.charAt(9) == '5').count()).isZero();
        // Trailer (64 ch, "9" na última posição) + linha de assinatura (100 ch).
        assertThat(linhas[linhas.length - 2]).hasSize(64).startsWith("999999999").endsWith("9");
        assertThat(linhas[linhas.length - 1].strip()).isEqualTo("ASSINATURA_DIGITAL_EM_ARQUIVO_P7S");

        AfdResponse aej = aejService.gerar(COMPETENCIA);
        assertThat(aej.totalRegistros()).isEqualTo(2);
        assertThat(aej.hashSha256()).isNotBlank();
        String[] linhasAej = aej.conteudo().split("\r\n");
        assertThat(linhasAej[0]).startsWith("01|").endsWith("|002"); // cabeçalho na versão vigente
        assertThat(linhasAej[1]).startsWith("02|1|3|");              // tpRep 3 = REP-P
        assertThat(java.util.Arrays.stream(linhasAej)
                .filter(l -> l.startsWith("05|")).count()).isEqualTo(2);
        assertThat(linhasAej[linhasAej.length - 2]).startsWith("99|");

        var conf = relatorioService.conformidadeIn008(COMPETENCIA);
        assertThat(conf.totalServidores()).isEqualTo(1);
        assertThat(conf.totalRegistros()).isEqualTo(2);
    }

    @Test
    void inclusaoDeServidorViraRegistroTipo5NoAfdDoMesEmQueOcorreu() {
        // A servidora foi cadastrada no setUp (agora), então o evento do ARP cai no mês corrente.
        String[] linhas = afdService.gerar(YearMonth.now()).conteudo().split("\r\n");

        var empregados = java.util.Arrays.stream(linhas)
                .filter(l -> l.length() > 9 && l.charAt(9) == '5').toList();
        assertThat(empregados).hasSize(1);

        String registro = empregados.get(0);
        assertThat(registro).hasSize(118);
        assertThat(registro.charAt(34)).as("operação de inclusão").isEqualTo('I');
        assertThat(registro.substring(35, 47)).isEqualTo("012345678901"); // CPF em 12 posições
        assertThat(registro.substring(47, 99)).startsWith("Joana");
        assertThat(registro.substring(114, 118)).matches("[0-9A-F]{4}");  // CRC-16
    }

    @Test
    void nsrEUnicoEntreMarcacoesEEventosDoRep() {
        // Anexo IX: a numeração é sequencial e ÚNICA por ente, contando TODAS as operações do REP.
        // Se empregados e marcações usassem sequências separadas, o AFD sairia com NSR repetido.
        java.util.List<String> nsrs = new java.util.ArrayList<>();
        for (String linha : afdService.gerar(YearMonth.now()).conteudo().split("\r\n")) {
            char tipo = linha.length() > 9 ? linha.charAt(9) : ' ';
            if (tipo == '5' || tipo == '6' || tipo == '7') {
                nsrs.add(linha.substring(0, 9));
            }
        }
        assertThat(nsrs).doesNotHaveDuplicates();
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
