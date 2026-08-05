package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.domain.ApuracaoDia;
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

/** Escala rotativa 12x36 (12.3.9): trabalha em dias alternados a partir da âncora da escala. */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class RotacaoEscalaTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate ANCORA = LocalDate.of(2026, 3, 2); // 1º dia de trabalho (delta 0)

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
        tenantId = tenantService.criar(new CriarTenantRequest("Ente P", "ente-p", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "70000000000", "Pedro Plantonista", null,
                List.of(new CriarVinculoRequest("M-P", Regime.ESTATUTARIO, "Plantonista", 40))));
        vinculoId = servidor.vinculos().get(0).id();

        // Escala 12x36 com turno 07:00–19:00; o dia da semana do horário é irrelevante na rotação.
        UUID jornadaId = jornadaService.criar(
                new CriarJornadaRequest("Plantão 12x36", TipoJornada.ESCALA_12X36, 2160, 10, 0)).id();
        jornadaService.definirHorarios(jornadaId, List.of(
                new HorarioRequest(1, LocalTime.of(7, 0), LocalTime.of(19, 0))));
        escalaService.atribuir(new CriarEscalaRequest(vinculoId, jornadaId, ANCORA, null));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void registrar(LocalDate data, TipoMarcacao tipo, int hora, int minuto) {
        Instant instante = data.atTime(hora, minuto).atZone(ZONE).toInstant();
        registroRepository.save(new RegistroPonto(tenantId, vinculoId, nsr.getAndIncrement(),
                tipo, OrigemRegistro.MOBILE, instante, instante, null, null, false,
                UUID.randomUUID().toString()));
    }

    private boolean temOcorrencia(ApuracaoDia ap, TipoOcorrencia tipo) {
        return ap.ocorrencias().stream().map(Ocorrencia::tipo).anyMatch(t -> t == tipo);
    }

    @Test
    void diasDeTrabalhoAlternamPelaParidadeDaAncora() {
        // Dia de trabalho (âncora, delta 0): sem batida ⇒ FALTA.
        assertThat(temOcorrencia(apuracaoService.apurarDia(vinculoId, ANCORA), TipoOcorrencia.FALTA)).isTrue();

        // Dia de descanso (delta 1): sem batida ⇒ NÃO há falta.
        assertThat(temOcorrencia(apuracaoService.apurarDia(vinculoId, ANCORA.plusDays(1)), TipoOcorrencia.FALTA)).isFalse();

        // Próximo dia de trabalho (delta 2): sem batida ⇒ FALTA.
        assertThat(temOcorrencia(apuracaoService.apurarDia(vinculoId, ANCORA.plusDays(2)), TipoOcorrencia.FALTA)).isTrue();
    }

    @Test
    void trabalhoNoDiaDeDescansoViraHoraExtra() {
        LocalDate descanso = ANCORA.plusDays(1); // delta 1 = descanso
        registrar(descanso, TipoMarcacao.ENTRADA, 8, 0);
        registrar(descanso, TipoMarcacao.SAIDA, 12, 0);

        ApuracaoDia ap = apuracaoService.apurarDia(vinculoId, descanso);
        assertThat(temOcorrencia(ap, TipoOcorrencia.HORA_EXTRA)).isTrue();
        assertThat(temOcorrencia(ap, TipoOcorrencia.FALTA)).isFalse();
    }
}
