package br.gov.ponto.me;

import br.gov.ponto.apuracao.ApuracaoService;
import br.gov.ponto.apuracao.JustificativaService;
import br.gov.ponto.apuracao.api.JustificativaResponse;
import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.apuracao.domain.Ocorrencia;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.ausencia.AusenciaService;
import br.gov.ponto.ausencia.api.AusenciaResponse;
import br.gov.ponto.bancohoras.BancoHorasService;
import br.gov.ponto.bancohoras.api.SaldoResponse;
import br.gov.ponto.biometria.BiometriaService;
import br.gov.ponto.biometria.api.VerificacaoResponse;
import br.gov.ponto.cadastro.RegrasPontoService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.comunicado.ComunicadoService;
import br.gov.ponto.comunicado.api.ComunicadoResponse;
import br.gov.ponto.correcao.CorrecaoService;
import br.gov.ponto.correcao.api.CorrecaoResponse;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.ia.ConsentimentoServidor;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.EspelhoService;
import br.gov.ponto.espelho.api.EspelhoResponse;
import br.gov.ponto.jornada.EscalaRepository;
import br.gov.ponto.jornada.JornadaHorarioRepository;
import br.gov.ponto.jornada.api.HorarioResponse;
import br.gov.ponto.lgpd.LgpdService;
import br.gov.ponto.lgpd.api.ExportacaoTitularResponse;
import br.gov.ponto.me.api.BaterMeRequest;
import br.gov.ponto.me.api.JustificativaMeRequest;
import br.gov.ponto.me.api.ResumoMeResponse;
import br.gov.ponto.notificacao.NotificacaoService;
import br.gov.ponto.notificacao.api.NotificacaoResponse;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.registro.api.BatidaResponse;
import br.gov.ponto.registro.api.ComprovanteResponse;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.satisfacao.SatisfacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Operações do app do servidor, sempre restritas ao {@code vinculoId} do dispositivo
 * autenticado (passado pelo controller a partir do {@code DispositivoPrincipal}).
 */
@Service
public class MeService implements ConsentimentoServidor {

    private final RegistroService registroService;
    private final EspelhoService espelhoService;
    private final BancoHorasService bancoHorasService;
    private final JustificativaService justificativaService;
    private final NotificacaoService notificacaoService;
    private final BiometriaService biometriaService;
    private final LgpdService lgpdService;
    private final RegrasPontoService regrasPontoService;
    private final ComunicadoService comunicadoService;
    private final ApuracaoService apuracaoService;
    private final CorrecaoService correcaoService;
    private final AusenciaService ausenciaService;
    private final SatisfacaoService satisfacaoService;
    private final VinculoRepository vinculoRepository;
    private final EscalaRepository escalaRepository;
    private final JornadaHorarioRepository horarioRepository;

    public MeService(RegistroService registroService, EspelhoService espelhoService,
                     BancoHorasService bancoHorasService, JustificativaService justificativaService,
                     NotificacaoService notificacaoService, BiometriaService biometriaService,
                     LgpdService lgpdService, RegrasPontoService regrasPontoService,
                     ComunicadoService comunicadoService, ApuracaoService apuracaoService,
                     CorrecaoService correcaoService, AusenciaService ausenciaService,
                     SatisfacaoService satisfacaoService,
                     VinculoRepository vinculoRepository, EscalaRepository escalaRepository,
                     JornadaHorarioRepository horarioRepository) {
        this.registroService = registroService;
        this.espelhoService = espelhoService;
        this.bancoHorasService = bancoHorasService;
        this.justificativaService = justificativaService;
        this.notificacaoService = notificacaoService;
        this.biometriaService = biometriaService;
        this.lgpdService = lgpdService;
        this.regrasPontoService = regrasPontoService;
        this.comunicadoService = comunicadoService;
        this.apuracaoService = apuracaoService;
        this.correcaoService = correcaoService;
        this.ausenciaService = ausenciaService;
        this.satisfacaoService = satisfacaoService;
        this.vinculoRepository = vinculoRepository;
        this.escalaRepository = escalaRepository;
        this.horarioRepository = horarioRepository;
    }

    public BatidaResponse bater(UUID vinculoId, BaterMeRequest req) {
        OrigemRegistro origem = req.origem() != null ? req.origem() : OrigemRegistro.MOBILE;
        return registroService.bater(new BaterPontoRequest(vinculoId, origem, req.dataHoraDispositivo(),
                req.latitude(), req.longitude(), req.offline(), req.idempotencyKey()));
    }

    public List<ComprovanteResponse> comprovantes(UUID vinculoId) {
        return registroService.listarPorVinculo(vinculoId);
    }

    public EspelhoResponse espelho(UUID vinculoId, YearMonth competencia) {
        return espelhoService.gerar(vinculoId, competencia);
    }

    public SaldoResponse bancoHoras(UUID vinculoId) {
        return new SaldoResponse(vinculoId, bancoHorasService.saldo(vinculoId));
    }

    /** Resumo "a seu favor": saldo de banco de horas + hora extra acumulada na semana corrente. */
    @Transactional(readOnly = true)
    public ResumoMeResponse resumo(UUID vinculoId) {
        int saldo = bancoHorasService.saldo(vinculoId);
        LocalDate hoje = LocalDate.now(TempoMunicipal.ZONE);
        LocalDate inicioSemana = hoje.with(java.time.DayOfWeek.MONDAY);
        int horaExtra = 0;
        for (LocalDate d = inicioSemana; !d.isAfter(hoje); d = d.plusDays(1)) {
            ApuracaoDia ap = apuracaoService.apurarDia(vinculoId, d);
            horaExtra += ap.ocorrencias().stream()
                    .filter(o -> o.tipo() == TipoOcorrencia.HORA_EXTRA)
                    .mapToInt(Ocorrencia::minutos).sum();
        }
        return new ResumoMeResponse(saldo, horaExtra);
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> jornada(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        return escalaRepository.findByVinculoIdAndTenantId(vinculoId, tenantId).stream()
                .filter(e -> e.vigenteEm(LocalDate.now(TempoMunicipal.ZONE)))
                .findFirst()
                .map(e -> horarioRepository.findByJornadaIdAndTenantId(e.getJornadaId(), tenantId).stream()
                        .map(h -> new HorarioResponse(h.getDiaSemana(), h.getHoraEntrada(), h.getHoraSaida()))
                        .toList())
                .orElseGet(List::of);
    }

    public JustificativaResponse solicitarJustificativa(UUID vinculoId, JustificativaMeRequest req) {
        return justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, req.tipo(), req.dataInicio(), req.dataFim(), req.motivo(), req.anexo()));
    }

    @Transactional(readOnly = true)
    public List<NotificacaoResponse> notificacoes(UUID vinculoId) {
        return notificacaoService.listar(servidorDoVinculo(vinculoId).toString()).stream()
                .map(NotificacaoResponse::from).toList();
    }

    /** Comunicados oficiais visíveis ao servidor: gerais + os do seu órgão. */
    @Transactional(readOnly = true)
    public List<ComunicadoResponse> comunicados(UUID vinculoId) {
        return comunicadoService.listarParaVinculo(vinculoId).stream()
                .map(ComunicadoResponse::from).toList();
    }

    /** "Esqueci de bater": o servidor solicita uma correção de marcação (aprovação da chefia/RH). */
    @Transactional
    public CorrecaoResponse solicitarCorrecao(UUID vinculoId, Instant dataHora, TipoMarcacao tipo, String motivo) {
        return CorrecaoResponse.from(correcaoService.solicitar(vinculoId, dataHora, tipo, motivo));
    }

    @Transactional(readOnly = true)
    public List<CorrecaoResponse> correcoes(UUID vinculoId) {
        return correcaoService.listarPorVinculo(vinculoId).stream().map(CorrecaoResponse::from).toList();
    }

    /** Minhas férias/licenças programadas (autoatendimento no app). */
    @Transactional(readOnly = true)
    public List<AusenciaResponse> minhasAusencias(UUID vinculoId) {
        return ausenciaService.porVinculo(vinculoId).stream().map(AusenciaResponse::from).toList();
    }

    /** Pesquisa de satisfação: o servidor avalia (1..5) a experiência com o ponto eletrônico. */
    @Transactional
    public void avaliarSatisfacao(UUID vinculoId, int nota, String comentario) {
        satisfacaoService.registrar(vinculoId, nota, comentario);
    }

    /** Verificacao biometrica 1:1 do proprio servidor (antifraude na batida). */
    @Transactional(readOnly = true)
    public VerificacaoResponse verificarFace(UUID vinculoId, String descritor) {
        return biometriaService.verificar(servidorDoVinculo(vinculoId), descritor);
    }

    // --- Autoatendimento LGPD (titular = servidor do dispositivo) ---

    @Transactional(readOnly = true)
    public ExportacaoTitularResponse meusDadosLgpd(UUID vinculoId) {
        return lgpdService.exportarDadosTitular(servidorDoVinculo(vinculoId));
    }

    @Transactional
    public void registrarConsentimento(UUID vinculoId, String finalidade, boolean concedido) {
        lgpdService.registrarConsentimento(servidorDoVinculo(vinculoId), finalidade, concedido);
    }

    @Transactional(readOnly = true)
    public boolean consentimento(UUID vinculoId, String finalidade) {
        return lgpdService.consentimentoVigente(servidorDoVinculo(vinculoId), finalidade);
    }

    /** Config do app: o órgão do vínculo exige verificação (biometria/PIN/desenho/facial) na batida? */
    @Transactional(readOnly = true)
    public boolean verificacaoObrigatoria(UUID vinculoId) {
        return regrasPontoService.regrasDoOrgaoDoVinculo(vinculoId).verificacaoObrigatoria();
    }

    private UUID servidorDoVinculo(UUID vinculoId) {
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoId, TenantContext.requireCurrent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo inexistente"));
        return v.getServidorId();
    }
}
