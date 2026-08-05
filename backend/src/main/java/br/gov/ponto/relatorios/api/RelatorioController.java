package br.gov.ponto.relatorios.api;

import br.gov.ponto.relatorios.AbonoRelatorioService;
import br.gov.ponto.relatorios.AdesaoService;
import br.gov.ponto.relatorios.AfdService;
import br.gov.ponto.relatorios.AnomaliaService;
import br.gov.ponto.relatorios.AlertaRiscoService;
import br.gov.ponto.ia.ResumoIaService;
import br.gov.ponto.ia.api.ResumoIaResponse;
import br.gov.ponto.relatorios.BiExecutivoService;
import br.gov.ponto.relatorios.DeteccaoService;
import br.gov.ponto.relatorios.DossieService;
import br.gov.ponto.relatorios.InconsistenciaService;
import br.gov.ponto.relatorios.IndicadoresService;
import br.gov.ponto.relatorios.IntegridadeService;
import br.gov.ponto.relatorios.PdfEspelhoService;
import br.gov.ponto.relatorios.PrevisaoService;
import br.gov.ponto.relatorios.RelatorioService;
import br.gov.ponto.relatorios.RoiService;
import br.gov.ponto.relatorios.SimuladorService;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final AfdService afdService;
    private final PdfEspelhoService pdfEspelhoService;
    private final IntegridadeService integridadeService;
    private final IndicadoresService indicadoresService;
    private final DeteccaoService deteccaoService;
    private final AlertaRiscoService alertaRiscoService;
    private final BiExecutivoService biExecutivoService;
    private final SimuladorService simuladorService;
    private final InconsistenciaService inconsistenciaService;
    private final AbonoRelatorioService abonoRelatorioService;
    private final AdesaoService adesaoService;
    private final DossieService dossieService;
    private final RoiService roiService;
    private final AnomaliaService anomaliaService;
    private final ResumoIaService resumoIaService;
    private final PrevisaoService previsaoService;

    public RelatorioController(RelatorioService relatorioService, AfdService afdService,
                               PdfEspelhoService pdfEspelhoService, IntegridadeService integridadeService,
                               IndicadoresService indicadoresService, DeteccaoService deteccaoService,
                               AlertaRiscoService alertaRiscoService, BiExecutivoService biExecutivoService,
                               SimuladorService simuladorService, InconsistenciaService inconsistenciaService,
                               AbonoRelatorioService abonoRelatorioService, AdesaoService adesaoService,
                               DossieService dossieService, RoiService roiService,
                               AnomaliaService anomaliaService, ResumoIaService resumoIaService,
                               PrevisaoService previsaoService) {
        this.relatorioService = relatorioService;
        this.afdService = afdService;
        this.pdfEspelhoService = pdfEspelhoService;
        this.integridadeService = integridadeService;
        this.indicadoresService = indicadoresService;
        this.deteccaoService = deteccaoService;
        this.alertaRiscoService = alertaRiscoService;
        this.biExecutivoService = biExecutivoService;
        this.simuladorService = simuladorService;
        this.inconsistenciaService = inconsistenciaService;
        this.abonoRelatorioService = abonoRelatorioService;
        this.adesaoService = adesaoService;
        this.dossieService = dossieService;
        this.roiService = roiService;
        this.anomaliaService = anomaliaService;
        this.resumoIaService = resumoIaService;
        this.previsaoService = previsaoService;
    }

    @GetMapping("/frequencia")
    public RelatorioFrequenciaResponse frequencia(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return relatorioService.frequenciaMensal(vinculoId, competencia);
    }

    @GetMapping(value = "/frequencia/csv", produces = "text/csv")
    public ResponseEntity<String> frequenciaCsv(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(relatorioService.exportarCsv(vinculoId, competencia));
    }

    @GetMapping("/afd")
    public AfdResponse afd(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return afdService.gerar(competencia);
    }

    /** AFD como arquivo de texto (download), no layout de largura fixa. */
    @GetMapping(value = "/afd/arquivo", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> afdArquivo(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        AfdResponse afd = afdService.gerar(competencia);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"AFD-" + competencia + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(afd.conteudo());
    }

    @GetMapping("/aej")
    public AfdResponse aej(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return afdService.gerarAej(competencia);
    }

    /** Espelho de ponto mensal em PDF (download). */
    @GetMapping(value = "/espelho/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> espelhoPdf(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        byte[] pdf = pdfEspelhoService.gerarPdf(vinculoId, competencia);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"espelho-" + competencia + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Verifica a integridade (tamper-evidence) da cadeia de registros do período. */
    @GetMapping("/integridade")
    public IntegridadeResponse integridade(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return integridadeService.verificar(competencia);
    }

    /** Indicadores de gestão/adesão do ente (painel do gestor + piloto). */
    @GetMapping("/indicadores")
    public IndicadoresResponse indicadores(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return indicadoresService.obter(competencia);
    }

    /**
     * Varredura de irregularidades para o controle interno: acúmulo ilícito de cargos
     * (jornadas sobrepostas) e "servidores fantasma" (vínculo ativo sem batidas no período).
     */
    @GetMapping("/acumulo")
    public DeteccaoResponse acumulo(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return deteccaoService.detectar(competencia);
    }

    /** Alertas de risco proativos: ajustes manuais de banco de horas + batidas fora da cerca. */
    @GetMapping("/alertas")
    public AlertasResponse alertas(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return alertaRiscoService.alertas(competencia);
    }

    /** BI executivo por secretaria: presença/absenteísmo, hora extra e fantasmas por órgão. */
    @GetMapping("/bi")
    public BiExecutivoResponse bi(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return biExecutivoService.porOrgao(competencia);
    }

    /**
     * Simulador orçamentário de hora extra + alerta de limite da LRF: "autorizar X horas
     * extras custa Y e me deixa a Z% da RCL". Valores em reais.
     */
    @GetMapping("/simulador")
    public SimulacaoResponse simulador(
            @RequestParam BigDecimal horasExtras,
            @RequestParam BigDecimal custoHora,
            @RequestParam BigDecimal gastoPessoalAtual,
            @RequestParam BigDecimal rcl) {
        return simuladorService.simular(horasExtras, custoHora, gastoPessoalAtual, rcl);
    }

    /**
     * Painel de ROI / economia em R$ (12.3.1): hora extra controlada + queda de absenteísmo,
     * combinando o apurado com os percentuais de redução informados pelo gestor.
     */
    @GetMapping("/roi")
    public RoiResponse roi(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
            @RequestParam BigDecimal custoHora,
            @RequestParam(defaultValue = "0") BigDecimal reducaoHoraExtraPct,
            @RequestParam(defaultValue = "0") BigDecimal custoDiaFalta,
            @RequestParam(defaultValue = "0") BigDecimal reducaoAbsenteismoPct) {
        return roiService.estimar(competencia, custoHora, reducaoHoraExtraPct,
                custoDiaFalta, reducaoAbsenteismoPct);
    }

    /** Dossiê de conformidade / escudo jurídico empacotado (IN008 + AFD + integridade + prazo TCM). */
    @GetMapping("/dossie")
    public DossieResponse dossie(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return dossieService.gerar(competencia);
    }

    /** Adesão por órgão (acompanhamento da implantação/piloto). */
    @GetMapping("/adesao/orgao")
    public AdesaoResponse adesaoPorOrgao() {
        return adesaoService.porOrgao();
    }

    /** Adesão por regime (isonomia: comissionados/chefias também batem ponto). */
    @GetMapping("/adesao/regime")
    public AdesaoResponse adesaoPorRegime() {
        return adesaoService.porRegime();
    }

    /** Inconsistências de jornada: dias com número ímpar de marcações (possível esquecimento). */
    @GetMapping("/inconsistencias")
    public InconsistenciasResponse inconsistencias(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return inconsistenciaService.detectar(competencia);
    }

    /** Relatório de abonos e exceções do período (com a decisão). */
    @GetMapping("/abonos")
    public List<AbonoResponse> abonos(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return abonoRelatorioService.abonos(competencia);
    }

    @GetMapping(value = "/abonos/csv", produces = "text/csv")
    public ResponseEntity<String> abonosCsv(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"abonos-" + competencia + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(abonoRelatorioService.exportarCsv(competencia));
    }

    /**
     * Anomalias heurísticas (sem IA), ligável no painel (funcionalidade ANOMALIAS): sinaliza
     * hora extra atípica. {@code habilitada=false} = a prefeitura mantém a função desligada.
     */
    @GetMapping("/anomalias")
    public AnomaliaResponse anomalias(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return anomaliaService.detectar(competencia);
    }

    /**
     * Resumo executivo em linguagem natural por IA (funcionalidade IA_RESUMO, ligável no painel).
     * {@code disponivel=false} = a prefeitura mantém a função desligada ou não há provedor.
     */
    @GetMapping("/resumo-ia")
    public ResumoIaResponse resumoIa(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return resumoIaService.resumir(competencia);
    }

    /**
     * Previsão de absenteísmo + custo de hora extra (12.4.12), heurística sobre os 3 meses
     * anteriores à competência. {@code custoHora} (opcional) monetiza a hora extra projetada.
     */
    @GetMapping("/previsao")
    public PrevisaoResponse previsao(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
            @RequestParam(defaultValue = "0") BigDecimal custoHora) {
        return previsaoService.prever(competencia, custoHora);
    }
}
