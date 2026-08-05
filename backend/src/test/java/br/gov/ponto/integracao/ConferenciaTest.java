package br.gov.ponto.integracao;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.integracao.api.ConferenciaRequest;
import br.gov.ponto.integracao.api.ConferenciaResponse;
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
import br.gov.ponto.relatorios.RelatorioService;
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
class ConferenciaTest {

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
    private RegistroPontoRepository registroRepository;
    @Autowired
    private ConferenciaFolhaService conferenciaFolhaService;
    @Autowired
    private RelatorioService relatorioService;

    private UUID tenantId;
    private UUID vinculoId;
    private final AtomicLong nsr = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Conf", "ente-conf", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var s = servidorService.criar(new CriarServidorRequest("15151515151", "Rui", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));
        vinculoId = s.vinculos().get(0).id();
        UUID jornada = jornadaService.criar(new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornada, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(vinculoId, jornada, DATA.minusMonths(1), null));
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 13, 0); // 60 min de hora extra
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
    void apontaDivergenciaQuandoFolhaNaoBateComApurado() {
        // Valores reais apurados na competência (hora extra do dia + faltas dos demais dias úteis).
        var freq = relatorioService.frequenciaMensal(vinculoId, COMPETENCIA);
        int heApurada = freq.minutosHoraExtra();
        int faltasApuradas = freq.qtdFaltas();
        assertThat(heApurada).isEqualTo(60); // confirma a hora extra do dia batido

        // Folha que NÃO bate com o apurado (hora extra a menos) → divergente.
        var divergente = conferenciaFolhaService.conferir(COMPETENCIA, new ConferenciaRequest(
                List.of(new ConferenciaRequest.ItemFolha(vinculoId, heApurada - 60, faltasApuradas))));
        assertThat(divergente.totalDivergencias()).isEqualTo(1);
        assertThat(divergente.itens().get(0).horaExtraApuradaMinutos()).isEqualTo(heApurada);
        assertThat(divergente.itens().get(0).divergente()).isTrue();

        // Folha que bate exatamente com o apurado → sem divergência.
        var ok = conferenciaFolhaService.conferir(COMPETENCIA, new ConferenciaRequest(
                List.of(new ConferenciaRequest.ItemFolha(vinculoId, heApurada, faltasApuradas))));
        assertThat(ok.totalDivergencias()).isZero();
        assertThat(ok.itens().get(0).divergente()).isFalse();
    }
}
