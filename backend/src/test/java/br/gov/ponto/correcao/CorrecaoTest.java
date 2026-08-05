package br.gov.ponto.correcao;

import br.gov.ponto.apuracao.ApuracaoService;
import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.correcao.api.CorrecaoLoteRequest;
import br.gov.ponto.correcao.domain.CorrecaoMarcacao;
import br.gov.ponto.correcao.domain.StatusCorrecao;
import br.gov.ponto.espelho.CompetenciaService;
import br.gov.ponto.jornada.EscalaService;
import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.relatorios.IntegridadeService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class CorrecaoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
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
    private CorrecaoService correcaoService;
    @Autowired
    private ApuracaoService apuracaoService;
    @Autowired
    private CompetenciaService competenciaService;
    @Autowired
    private RegistroPontoRepository registroRepository;
    @Autowired
    private RegistroService registroService;
    @Autowired
    private IntegridadeService integridadeService;

    private UUID tenantId;
    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Cor", "ente-cor", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var servidor = servidorService.criar(new CriarServidorRequest(
                "10101010101", "Rui", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));
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

    private Instant em(int hora, int minuto) {
        return DATA.atTime(hora, minuto).atZone(ZONE).toInstant();
    }

    @Test
    void esqueciDeBaterAprovadoCriaMarcacaoEncadeada() {
        CorrecaoMarcacao solicitada = correcaoService.solicitar(vinculoId, em(8, 0), TipoMarcacao.ENTRADA, "Esqueci de bater");
        assertThat(solicitada.getStatus()).isEqualTo(StatusCorrecao.PENDENTE);
        assertThat(correcaoService.listarPendentes()).hasSize(1);

        CorrecaoMarcacao aprovada = correcaoService.aprovar(solicitada.getId(), "Deferido");
        assertThat(aprovada.getStatus()).isEqualTo(StatusCorrecao.APROVADA);
        assertThat(aprovada.getRegistroId()).isNotNull();

        var registros = registroRepository.findByVinculoIdAndTenantIdOrderByNsr(vinculoId, tenantId);
        assertThat(registros).hasSize(1);
        assertThat(registros.get(0).getOrigem()).isEqualTo(OrigemRegistro.AJUSTE);
        assertThat(registros.get(0).getDataHoraServidor()).isEqualTo(em(8, 0));
        assertThat(correcaoService.listarPendentes()).isEmpty();
    }

    @Test
    void correcaoEmLoteDoRhCriaMarcacoesERefleteNaApuracao() {
        // Sem batidas, o dia seria FALTA.
        assertThat(apuracaoService.apurarDia(vinculoId, DATA).ocorrencias())
                .anyMatch(o -> o.tipo() == TipoOcorrencia.FALTA);

        var itens = List.of(
                new CorrecaoLoteRequest.Item(em(8, 0), TipoMarcacao.ENTRADA),
                new CorrecaoLoteRequest.Item(em(12, 0), TipoMarcacao.SAIDA));
        var criadas = correcaoService.corrigirEmLote(vinculoId, itens, "Relógio em manutenção");
        assertThat(criadas).hasSize(2);
        assertThat(criadas).allMatch(c -> c.getStatus() == StatusCorrecao.APROVADA);

        ApuracaoDia ap = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(ap.ocorrencias()).noneMatch(o -> o.tipo() == TipoOcorrencia.FALTA);
        assertThat(ap.minutosTrabalhados()).isEqualTo(240);
    }

    @Test
    void correcaoRejeitadaNaoCriaMarcacao() {
        CorrecaoMarcacao solicitada = correcaoService.solicitar(vinculoId, em(8, 0), TipoMarcacao.ENTRADA, "Esqueci");
        correcaoService.rejeitar(solicitada.getId(), "Sem comprovação");
        assertThat(correcaoService.listarPorVinculo(vinculoId).get(0).getStatus())
                .isEqualTo(StatusCorrecao.REJEITADA);
        assertThat(registroRepository.findByVinculoIdAndTenantIdOrderByNsr(vinculoId, tenantId)).isEmpty();
    }

    @Test
    void correcaoEmCompetenciaFechadaEhRejeitada() {
        competenciaService.fechar(vinculoId, COMPETENCIA);
        assertThatThrownBy(() ->
                correcaoService.solicitar(vinculoId, em(8, 0), TipoMarcacao.ENTRADA, "Esqueci"))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void correcaoRetroativaNaoQuebraOVerificadorDeIntegridade() {
        // NSR 1 = correção de março; NSR 2 = abril (entre as duas de março);
        // NSR 3 = março de novo. A janela de março fica não-contígua em NSR (pula o NSR de abril).
        Instant mar1 = LocalDate.of(2026, 3, 2).atTime(8, 0).atZone(ZONE).toInstant();
        Instant abr = LocalDate.of(2026, 4, 6).atTime(8, 0).atZone(ZONE).toInstant();
        Instant mar2 = LocalDate.of(2026, 3, 2).atTime(12, 0).atZone(ZONE).toInstant();
        registroService.registrarCorrecao(vinculoId, mar1, TipoMarcacao.ENTRADA, "m1");
        registroService.registrarCorrecao(vinculoId, abr, TipoMarcacao.ENTRADA, "abr");
        registroService.registrarCorrecao(vinculoId, mar2, TipoMarcacao.SAIDA, "m2");

        // A cadeia continua íntegra mesmo com a janela de março não-contígua em NSR.
        var marco = integridadeService.verificar(YearMonth.of(2026, 3));
        assertThat(marco.integra()).isTrue();
        var abril = integridadeService.verificar(YearMonth.of(2026, 4));
        assertThat(abril.integra()).isTrue();
    }
}
