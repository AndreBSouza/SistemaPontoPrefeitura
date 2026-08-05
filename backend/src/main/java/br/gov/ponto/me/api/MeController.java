package br.gov.ponto.me.api;

import br.gov.ponto.apuracao.api.DecisaoRequest;
import br.gov.ponto.apuracao.api.JustificativaResponse;
import br.gov.ponto.ativacao.DispositivoPrincipal;
import br.gov.ponto.ausencia.api.AusenciaResponse;
import br.gov.ponto.bancohoras.api.SaldoResponse;
import br.gov.ponto.biometria.api.VerificacaoResponse;
import br.gov.ponto.comunicado.api.ComunicadoResponse;
import br.gov.ponto.correcao.api.CorrecaoResponse;
import br.gov.ponto.correcao.api.SolicitarCorrecaoRequest;
import br.gov.ponto.satisfacao.api.RegistrarSatisfacaoRequest;
import br.gov.ponto.espelho.api.EspelhoResponse;
import br.gov.ponto.ia.AssistenteService;
import br.gov.ponto.ia.AtestadoOcrService;
import br.gov.ponto.ia.api.AssistenteResponse;
import br.gov.ponto.ia.api.OcrAtestadoResponse;
import br.gov.ponto.ia.api.PerguntaRequest;
import br.gov.ponto.jornada.api.HorarioResponse;
import br.gov.ponto.lgpd.api.ExportacaoTitularResponse;
import br.gov.ponto.me.CarteiraService;
import br.gov.ponto.me.ComprovanteRepService;
import br.gov.ponto.me.MeService;
import br.gov.ponto.relatorios.PdfEspelhoService;
import org.springframework.web.multipart.MultipartFile;
import br.gov.ponto.tenant.BrandingService;
import br.gov.ponto.tenant.api.BrandingResponse;
import br.gov.ponto.registro.api.BatidaResponse;
import br.gov.ponto.registro.api.ComprovanteResponse;
import br.gov.ponto.notificacao.api.NotificacaoResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * API do app do servidor autenticado por dispositivo. O vinculo vem sempre do
 * {@link DispositivoPrincipal} (token), nunca de parametro — o servidor só vê o seu.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;
    private final ComprovanteRepService comprovanteRepService;
    private final BrandingService brandingService;
    private final PdfEspelhoService pdfEspelhoService;
    private final CarteiraService carteiraService;
    private final br.gov.ponto.me.TrilhaService trilhaService;
    private final br.gov.ponto.me.GestorService gestorService;
    private final AssistenteService assistenteService;
    private final AtestadoOcrService atestadoOcrService;

    public MeController(MeService meService, BrandingService brandingService,
                        PdfEspelhoService pdfEspelhoService, CarteiraService carteiraService,
                        br.gov.ponto.me.TrilhaService trilhaService,
                        br.gov.ponto.me.GestorService gestorService,
                        AssistenteService assistenteService,
                        AtestadoOcrService atestadoOcrService,
                        ComprovanteRepService comprovanteRepService) {
        this.meService = meService;
        this.comprovanteRepService = comprovanteRepService;
        this.brandingService = brandingService;
        this.pdfEspelhoService = pdfEspelhoService;
        this.carteiraService = carteiraService;
        this.trilhaService = trilhaService;
        this.gestorService = gestorService;
        this.assistenteService = assistenteService;
        this.atestadoOcrService = atestadoOcrService;
    }

    /** Assistente/chatbot do servidor (IA opcional, ligável por ente no painel). */
    @PostMapping("/assistente")
    public AssistenteResponse assistente(@AuthenticationPrincipal DispositivoPrincipal me,
                                         @Valid @RequestBody PerguntaRequest req) {
        return assistenteService.perguntar(req.pergunta(), me.vinculoId());
    }

    /**
     * OCR de atestado médico p/ pré-preencher a justificativa (IA opcional, ligável por ente).
     * Exige consentimento do servidor (dado de saúde/LGPD) — sem ele, retorna 403
     * {@code CONSENTIMENTO_NECESSARIO} e o app pede o consentimento via POST /api/me/lgpd/consentimento.
     */
    @PostMapping("/atestados/ocr")
    public OcrAtestadoResponse ocrAtestado(@AuthenticationPrincipal DispositivoPrincipal me,
                                           @RequestParam("arquivo") MultipartFile arquivo) throws java.io.IOException {
        return atestadoOcrService.ler(me.vinculoId(), arquivo.getBytes(), arquivo.getContentType());
    }

    /** Config do app (ex.: exige verificação na batida) para o vínculo do dispositivo. */
    @GetMapping("/config")
    public ConfigMeResponse config(@AuthenticationPrincipal DispositivoPrincipal me) {
        return new ConfigMeResponse(meService.verificacaoObrigatoria(me.vinculoId()));
    }

    /** Identidade visual do ente (white-label) para o app se personalizar. */
    @GetMapping("/branding")
    public BrandingResponse branding() {
        return brandingService.obter();
    }

    /** Carteira funcional digital do servidor (dados do vínculo + ente). */
    @GetMapping("/carteira")
    public CarteiraResponse carteira(@AuthenticationPrincipal DispositivoPrincipal me) {
        return carteiraService.carteira(me.vinculoId());
    }

    /** Declaração de frequência (PDF) que o próprio servidor emite (banco, faculdade…). */
    @GetMapping(value = "/declaracao", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> declaracao(@AuthenticationPrincipal DispositivoPrincipal me,
                                             @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        byte[] pdf = pdfEspelhoService.gerarDeclaracao(me.vinculoId(), competencia);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"declaracao-" + competencia + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Botão único: deduz o tipo da batida para o vinculo do dispositivo. */
    @PostMapping("/bater")
    public ResponseEntity<BatidaResponse> bater(@AuthenticationPrincipal DispositivoPrincipal me,
                                                @Valid @RequestBody BaterMeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meService.bater(me.vinculoId(), request));
    }

    @GetMapping("/comprovantes")
    public List<ComprovanteResponse> comprovantes(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.comprovantes(me.vinculoId());
    }

    /**
     * Comprovante de Registro de Ponto do Trabalhador de uma marcacao (art. 79), com o codigo
     * hash SHA-256 exigido para o REP-P.
     */
    @GetMapping("/comprovantes/{nsr}")
    public ComprovanteRepResponse comprovante(@AuthenticationPrincipal DispositivoPrincipal me,
                                              @PathVariable long nsr) {
        return comprovanteRepService.porNsr(me.vinculoId(), nsr);
    }

    @GetMapping("/espelho")
    public EspelhoResponse espelho(@AuthenticationPrincipal DispositivoPrincipal me,
                                   @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return meService.espelho(me.vinculoId(), competencia);
    }

    @GetMapping("/banco-horas")
    public SaldoResponse bancoHoras(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.bancoHoras(me.vinculoId());
    }

    /** Resumo "a seu favor": saldo de banco de horas + hora extra da semana (12.1.5). */
    @GetMapping("/resumo")
    public ResumoMeResponse resumo(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.resumo(me.vinculoId());
    }

    /** Horários da jornada vigente — usado pelo app para agendar os lembretes. */
    @GetMapping("/jornada")
    public List<HorarioResponse> jornada(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.jornada(me.vinculoId());
    }

    @PostMapping("/justificativas")
    public ResponseEntity<JustificativaResponse> justificar(@AuthenticationPrincipal DispositivoPrincipal me,
                                                            @Valid @RequestBody JustificativaMeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meService.solicitarJustificativa(me.vinculoId(), request));
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoResponse> notificacoes(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.notificacoes(me.vinculoId());
    }

    /** Comunicados oficiais (broadcast) da prefeitura visíveis ao servidor: gerais + do seu órgão. */
    @GetMapping("/comunicados")
    public List<ComunicadoResponse> comunicados(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.comunicados(me.vinculoId());
    }

    /** "Esqueci de bater": solicita correção de marcação (aprovação da chefia/RH). */
    @PostMapping("/correcoes")
    public ResponseEntity<CorrecaoResponse> solicitarCorrecao(
            @AuthenticationPrincipal DispositivoPrincipal me,
            @Valid @RequestBody SolicitarCorrecaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                meService.solicitarCorrecao(me.vinculoId(), request.dataHora(), request.tipo(), request.motivo()));
    }

    @GetMapping("/correcoes")
    public List<CorrecaoResponse> correcoes(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.correcoes(me.vinculoId());
    }

    /** Trilha pessoal (12.1.2): histórico do que aconteceu com meus registros e quem decidiu. */
    @GetMapping("/trilha")
    public List<TrilhaEvento> trilha(@AuthenticationPrincipal DispositivoPrincipal me) {
        return trilhaService.montar(me.vinculoId());
    }

    // --- App do gestor (12.3.11): só atua se o servidor do dispositivo for chefia ---

    /** Resumo de gestão: se o servidor é chefia e quantas pendências do time há. */
    @GetMapping("/chefia")
    public GestorResumoResponse chefia(@AuthenticationPrincipal DispositivoPrincipal me) {
        boolean souGestor = gestorService.souGestor(me.vinculoId());
        int pendentes = souGestor ? gestorService.pendentesDoMeuTime(me.vinculoId()).size() : 0;
        return new GestorResumoResponse(souGestor, pendentes);
    }

    /** Justificativas pendentes do time do gestor. */
    @GetMapping("/chefia/pendentes")
    public List<JustificativaResponse> pendentesDoTime(@AuthenticationPrincipal DispositivoPrincipal me) {
        return gestorService.pendentesDoMeuTime(me.vinculoId());
    }

    /** Aprova uma justificativa do time (com alçada de gestor; só do próprio time). */
    @PostMapping("/chefia/justificativas/{id}/aprovar")
    public JustificativaResponse aprovarDoTime(@AuthenticationPrincipal DispositivoPrincipal me,
                                               @PathVariable java.util.UUID id,
                                               @RequestBody(required = false) DecisaoRequest decisao) {
        return gestorService.aprovar(me.vinculoId(), id, decisao == null ? null : decisao.motivoDecisao());
    }

    @PostMapping("/chefia/justificativas/{id}/rejeitar")
    public JustificativaResponse rejeitarDoTime(@AuthenticationPrincipal DispositivoPrincipal me,
                                                @PathVariable java.util.UUID id,
                                                @RequestBody(required = false) DecisaoRequest decisao) {
        return gestorService.rejeitar(me.vinculoId(), id, decisao == null ? null : decisao.motivoDecisao());
    }

    /** Minhas férias/licenças programadas. */
    @GetMapping("/ausencias")
    public List<AusenciaResponse> ausencias(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.minhasAusencias(me.vinculoId());
    }

    /** Pesquisa de satisfação: o servidor avalia a experiência (1..5). */
    @PostMapping("/satisfacao")
    public ResponseEntity<Void> avaliarSatisfacao(
            @AuthenticationPrincipal DispositivoPrincipal me,
            @Valid @RequestBody RegistrarSatisfacaoRequest request) {
        meService.avaliarSatisfacao(me.vinculoId(), request.nota(), request.comentario());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Verificacao biometrica 1:1 do proprio servidor (antifraude na batida). */
    @PostMapping("/verificar-face")
    public VerificacaoResponse verificarFace(@AuthenticationPrincipal DispositivoPrincipal me,
                                             @Valid @RequestBody VerificarFaceRequest request) {
        return meService.verificarFace(me.vinculoId(), request.descritor());
    }

    // --- Autoatendimento LGPD: o servidor acessa/gerencia os próprios dados ---

    /** Exporta os dados do titular (direito de acesso, LGPD art. 18). */
    @GetMapping("/lgpd/dados")
    public ExportacaoTitularResponse meusDados(@AuthenticationPrincipal DispositivoPrincipal me) {
        return meService.meusDadosLgpd(me.vinculoId());
    }

    @GetMapping("/lgpd/consentimento")
    public ConsentimentoResponse consentimento(@AuthenticationPrincipal DispositivoPrincipal me,
                                               @RequestParam String finalidade) {
        return new ConsentimentoResponse(finalidade, meService.consentimento(me.vinculoId(), finalidade));
    }

    /** Concede ou revoga consentimento (ex.: uso de biometria). */
    @PostMapping("/lgpd/consentimento")
    public ConsentimentoResponse registrarConsentimento(@AuthenticationPrincipal DispositivoPrincipal me,
                                                         @Valid @RequestBody ConsentimentoMeRequest request) {
        meService.registrarConsentimento(me.vinculoId(), request.finalidade(), request.concedido());
        return new ConsentimentoResponse(request.finalidade(), request.concedido());
    }
}
