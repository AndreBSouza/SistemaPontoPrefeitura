package br.gov.ponto.relatorios;

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
import br.gov.ponto.relatorios.api.DeteccaoResponse;
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

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class DeteccaoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
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
    private DeteccaoService deteccaoService;
    @Autowired
    private RegistroPontoRepository registroRepository;

    private UUID tenantId;
    private UUID jornadaSegId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Det", "ente-det", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        // Jornada com horário às segundas 08:00–12:00 (base para detectar sobreposição).
        jornadaSegId = jornadaService.criar(
                new CriarJornadaRequest("Seg 8-12", TipoJornada.FIXA, 1200, 5, 0)).id();
        jornadaService.definirHorarios(jornadaSegId, List.of(
                new HorarioRequest(1, LocalTime.of(8, 0), LocalTime.of(12, 0))));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void detectaAcumuloDeCargosComJornadasSobrepostas() {
        // Servidor com DOIS vínculos ativos, ambos na mesma jornada (segundas 08–12) => conflito.
        var maria = servidorService.criar(new CriarServidorRequest(
                "11111111111", "Maria", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Professora", 20),
                        new CriarVinculoRequest("M-2", Regime.ESTATUTARIO, "Coordenadora", 20))));
        UUID v1 = maria.vinculos().get(0).id();
        UUID v2 = maria.vinculos().get(1).id();
        escalaService.atribuir(new CriarEscalaRequest(v1, jornadaSegId, COMPETENCIA.atDay(1).minusMonths(1), null));
        escalaService.atribuir(new CriarEscalaRequest(v2, jornadaSegId, COMPETENCIA.atDay(1).minusMonths(1), null));

        DeteccaoResponse r = deteccaoService.detectar(COMPETENCIA);
        assertThat(r.acumulos()).hasSize(1);
        assertThat(r.acumulos().get(0).servidorId()).isEqualTo(maria.id());
        assertThat(List.of(r.acumulos().get(0).vinculoA(), r.acumulos().get(0).vinculoB()))
                .containsExactlyInAnyOrder(v1, v2);
    }

    @Test
    void apontaVinculoSemBatidaComoFantasmaEPreservaQuemBate() {
        // João: vínculo ativo sem nenhuma batida => fantasma.
        var joao = servidorService.criar(new CriarServidorRequest(
                "22222222222", "João", null,
                List.of(new CriarVinculoRequest("J-1", Regime.ESTATUTARIO, "Auxiliar", 40))));
        UUID vJoao = joao.vinculos().get(0).id();

        // Ana: vínculo ativo COM batida no período => não é fantasma.
        var ana = servidorService.criar(new CriarServidorRequest(
                "33333333333", "Ana", null,
                List.of(new CriarVinculoRequest("A-1", Regime.ESTATUTARIO, "Fiscal", 40))));
        UUID vAna = ana.vinculos().get(0).id();
        Instant instante = COMPETENCIA.atDay(2).atTime(8, 0).atZone(ZONE).toInstant();
        registroRepository.save(new RegistroPonto(tenantId, vAna, 1L, TipoMarcacao.ENTRADA,
                OrigemRegistro.MOBILE, instante, instante, null, null, false, UUID.randomUUID().toString()));

        DeteccaoResponse r = deteccaoService.detectar(COMPETENCIA);
        assertThat(r.fantasmas()).extracting(DeteccaoResponse.Fantasma::vinculoId).contains(vJoao);
        assertThat(r.fantasmas()).extracting(DeteccaoResponse.Fantasma::vinculoId).doesNotContain(vAna);
    }
}
