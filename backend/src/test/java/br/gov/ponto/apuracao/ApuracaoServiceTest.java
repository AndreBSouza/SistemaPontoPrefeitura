package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.domain.Ocorrencia;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
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
class ApuracaoServiceTest {

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
    private RegistroPontoRepository registroRepository;

    private UUID tenantId;
    private UUID vinculoId;
    private final AtomicLong nsr = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Z", "ente-z", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "66666666666", "Fabio", null,
                List.of(new CriarVinculoRequest("M-7", Regime.ESTATUTARIO, "Tecnico", 40))));
        vinculoId = servidor.vinculos().get(0).id();

        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Manha 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornadaId, List.of(
                new HorarioRequest(DATA.getDayOfWeek().getValue(),
                        LocalTime.of(8, 0), LocalTime.of(12, 0))));
        escalaService.atribuir(new CriarEscalaRequest(
                vinculoId, jornadaId, DATA.minusMonths(1), null));
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
    void apuraAtraso() {
        registrar(TipoMarcacao.ENTRADA, 8, 10);
        registrar(TipoMarcacao.SAIDA, 12, 0);

        var apuracao = apuracaoService.apurarDia(vinculoId, DATA);

        assertThat(apuracao.minutosTrabalhados()).isEqualTo(230);
        assertThat(apuracao.minutosEsperados()).isEqualTo(240);
        assertThat(apuracao.ocorrencias())
                .extracting(Ocorrencia::tipo)
                .contains(TipoOcorrencia.ATRASO);
        assertThat(ocorrencia(apuracao.ocorrencias(), TipoOcorrencia.ATRASO)).isEqualTo(10);
    }

    @Test
    void apuraFaltaQuandoSemRegistros() {
        var apuracao = apuracaoService.apurarDia(vinculoId, DATA);

        assertThat(apuracao.minutosTrabalhados()).isZero();
        assertThat(apuracao.ocorrencias())
                .extracting(Ocorrencia::tipo)
                .containsExactly(TipoOcorrencia.FALTA);
    }

    @Test
    void apuraHoraExtra() {
        registrar(TipoMarcacao.ENTRADA, 8, 0);
        registrar(TipoMarcacao.SAIDA, 13, 0);

        var apuracao = apuracaoService.apurarDia(vinculoId, DATA);

        assertThat(apuracao.minutosTrabalhados()).isEqualTo(300);
        assertThat(ocorrencia(apuracao.ocorrencias(), TipoOcorrencia.HORA_EXTRA)).isEqualTo(60);
    }

    private int ocorrencia(List<Ocorrencia> ocorrencias, TipoOcorrencia tipo) {
        return ocorrencias.stream().filter(o -> o.tipo() == tipo)
                .mapToInt(Ocorrencia::minutos).findFirst().orElse(-1);
    }
}
