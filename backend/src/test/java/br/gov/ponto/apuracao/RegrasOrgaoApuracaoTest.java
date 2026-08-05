package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.bancohoras.BancoHorasService;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaService;
import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.api.BaterPontoRequest;
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

import java.math.BigDecimal;
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
class RegrasOrgaoApuracaoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATA = LocalDate.of(2026, 3, 2);

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
    private BancoHorasService bancoHorasService;
    @Autowired
    private RegistroPontoRepository registroRepository;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private RegistroService registroService;

    private UUID tenantId;
    private UUID vinculoId;
    private UUID orgaoId;
    private final AtomicLong nsr = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente RO", "ente-ro", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "77777777777", "Otávio", null,
                List.of(new CriarVinculoRequest("M-3", Regime.ESTATUTARIO, "Fiscal", 40))));
        vinculoId = servidor.vinculos().get(0).id();

        // Jornada manhã 08:00–12:00, tolerância própria de 5 min.
        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornadaId, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(), LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(vinculoId, jornadaId, DATA.minusMonths(1), null));

        // Vínculo lotado em um órgão (regras definidas em cada teste).
        orgaoId = lotacaoService.criar("Secretaria de Fiscalização", "SEF").getId();
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
    void toleranciaDoOrgaoTemPrecedenciaSobreAJornada() {
        // Órgão tolera 20 min; jornada tolera só 5. Entrada 10 min atrasada.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, 20, true, null, null, null));
        registrar(TipoMarcacao.ENTRADA, 8, 10);
        registrar(TipoMarcacao.SAIDA, 12, 0);

        ApuracaoDia ap = apuracaoService.apurarDia(vinculoId, DATA);
        // 10 min ≤ tolerância do órgão (20) => sem ATRASO (com a da jornada (5) haveria).
        assertThat(ap.ocorrencias()).noneMatch(o -> o.tipo() == TipoOcorrencia.ATRASO);
    }

    @Test
    void bancoHorasDesabilitadoNoOrgaoNaoLanca() {
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 13, 0); // 300 trabalhados, 240 esperados => +60

        // Desabilitado no órgão: não lança, saldo permanece zero.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, false, null, null, null));
        assertThat(bancoHorasService.lancarApuracaoDoDia(vinculoId, DATA)).isZero();

        // Habilitado: credita a hora extra.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true, null, null, null));
        assertThat(bancoHorasService.lancarApuracaoDoDia(vinculoId, DATA)).isEqualTo(60);
    }

    @Test
    void batidaForaDaCercaNaoPenalizaNaApuracao() {
        // "Fora da área" é só verificação do admin (fica no registro) — não vira ocorrência.
        Instant instante = DATA.atTime(8, 0).atZone(ZONE).toInstant();
        registroRepository.save(new RegistroPonto(tenantId, vinculoId, nsr.getAndIncrement(),
                TipoMarcacao.ENTRADA, OrigemRegistro.MOBILE, instante, instante, null, null, false,
                UUID.randomUUID().toString(), true)); // fora da cerca
        registrar(TipoMarcacao.SAIDA, 12, 0);

        ApuracaoDia ap = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(ap.ocorrencias()).noneMatch(o -> o.tipo().penalizaServidor());
    }

    @Test
    void baterForaDaCercaNuncaBloqueiaApenasRegistraParaAdmin() {
        // Cerca do órgão configurada, mas a geofence é só verificação: nunca bloqueia o servidor;
        // apenas marca o registro (com a localização) para o admin conferir.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true,
                new BigDecimal("-16.6800"), new BigDecimal("-49.2500"), 100));
        var req = new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE, null,
                new BigDecimal("-16.7000"), new BigDecimal("-49.3000"), false, "fora-sinal");

        registroService.bater(req); // não lança
        var reg = registroRepository.findByTenantIdAndIdempotencyKey(tenantId, "fora-sinal").orElseThrow();
        assertThat(reg.isForaDaCerca()).isTrue();
        assertThat(reg.getLatitude()).isEqualByComparingTo("-16.7000"); // localização guardada p/ o admin
    }

    @Test
    void teletrabalhoIgnoraGeofence() {
        // Órgão com cerca (que normalmente sinalizaria fora) MAS em teletrabalho → não sinaliza.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true,
                new BigDecimal("-16.6800"), new BigDecimal("-49.2500"), 100,
                null, null, null, true));
        var req = new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE, null,
                new BigDecimal("-16.7000"), new BigDecimal("-49.3000"), false, "teletrab");

        registroService.bater(req); // longe da cerca, mas teletrabalho
        var reg = registroRepository.findByTenantIdAndIdempotencyKey(tenantId, "teletrab").orElseThrow();
        assertThat(reg.isForaDaCerca()).isFalse();
    }

    @Test
    void tetoBancoHorasDoOrgaoLimitaAcumulo() {
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 13, 0); // 300 trabalhados, 240 esperados => +60

        // Órgão com banco habilitado e teto de 30 min: o crédito é limitado ao teto.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true, null, null, null, 30));
        assertThat(bancoHorasService.lancarApuracaoDoDia(vinculoId, DATA)).isEqualTo(30);
    }

    @Test
    void modoAdaptacaoSuprimePenalidadesEMarcaApuracao() {
        // Entrada 30 min atrasada (além da tolerância de 5) => ATRASO fora da adaptação.
        registrar(TipoMarcacao.ENTRADA, 8, 30);
        registrar(TipoMarcacao.SAIDA, 12, 0);

        // Órgão em adaptação até depois da DATA: só registra, sem penalizar.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true, null, null, null,
                null, null, DATA.plusDays(10)));
        ApuracaoDia emAdaptacao = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(emAdaptacao.emAdaptacao()).isTrue();
        assertThat(emAdaptacao.ocorrencias()).noneMatch(o -> o.tipo().penalizaServidor());

        // Encerrada a adaptação (vigência expirada), o atraso volta a ser apurado.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true, null, null, null,
                null, null, DATA.minusDays(1)));
        ApuracaoDia apos = apuracaoService.apurarDia(vinculoId, DATA);
        assertThat(apos.emAdaptacao()).isFalse();
        assertThat(apos.ocorrencias()).anyMatch(o -> o.tipo() == TipoOcorrencia.ATRASO);
    }

    @Test
    void modoAdaptacaoMantemCreditoDeHoraExtraNoBancoDeHoras() {
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 13, 0); // 300 trabalhados, 240 esperados => +60 (a favor)

        // Durante a adaptação o crédito a favor do servidor segue valendo.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true, null, null, null,
                null, null, DATA.plusDays(10)));
        assertThat(bancoHorasService.lancarApuracaoDoDia(vinculoId, DATA)).isEqualTo(60);
    }
}
