package br.gov.ponto.calendario;

import br.gov.ponto.apuracao.ApuracaoService;
import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.calendario.domain.TipoEventoCalendario;
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
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class CalendarioTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATA = LocalDate.of(2026, 3, 2); // segunda-feira

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
    private CalendarioService calendarioService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private RegistroPontoRepository registroRepository;

    private UUID tenantId;
    private UUID vinculoId;
    private UUID orgaoId;
    private final AtomicLong nsr = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Cal", "ente-cal", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "55555555555", "Lia", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));
        vinculoId = servidor.vinculos().get(0).id();

        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornadaId, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(vinculoId, jornadaId, DATA.minusMonths(1), null));

        orgaoId = lotacaoService.criar("Secretaria", "SEC").getId();
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId).orElseThrow();
        v.setLotacaoId(orgaoId);
        vinculoRepository.save(v);
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
    void feriadoGeralNeutralizaFaltaETornaDiaNaoUtil() {
        // Sem batidas: normalmente seria FALTA.
        ApuracaoDia semFeriado = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(semFeriado.ocorrencias()).anyMatch(o -> o.tipo() == TipoOcorrencia.FALTA);

        calendarioService.criar(DATA, TipoEventoCalendario.FERIADO, "Aniversário da cidade", null);

        ApuracaoDia comFeriado = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(comFeriado.diaUtil()).isFalse();
        assertThat(comFeriado.minutosEsperados()).isZero();
        assertThat(comFeriado.ocorrencias()).noneMatch(o -> o.tipo() == TipoOcorrencia.FALTA);
    }

    @Test
    void trabalhoNoFeriadoViraHoraExtra() {
        calendarioService.criar(DATA, TipoEventoCalendario.FERIADO, "Feriado municipal", null);
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 12, 0); // 240 min trabalhados num feriado

        ApuracaoDia ap = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(ap.minutosEsperados()).isZero();
        assertThat(ap.ocorrencias()).anyMatch(o -> o.tipo() == TipoOcorrencia.HORA_EXTRA && o.minutos() == 240);
    }

    @Test
    void abonoColetivoSoValeParaOOrgaoAlvo() {
        UUID outroOrgao = lotacaoService.criar("Outra Secretaria", "OUT").getId();

        // Abono coletivo direcionado a OUTRO órgão: não afeta o vínculo (segue FALTA).
        calendarioService.criar(DATA, TipoEventoCalendario.ABONO_COLETIVO, "Interdição do prédio anexo", outroOrgao);
        assertThat(apuracaoService.apurarDia(vinculoId, DATA).ocorrencias())
                .anyMatch(o -> o.tipo() == TipoOcorrencia.FALTA);

        // Abono para o órgão do vínculo: neutraliza a falta.
        calendarioService.criar(DATA, TipoEventoCalendario.ABONO_COLETIVO, "Interdição do prédio", orgaoId);
        assertThat(apuracaoService.apurarDia(vinculoId, DATA).ocorrencias())
                .noneMatch(o -> o.tipo() == TipoOcorrencia.FALTA);
    }
}
