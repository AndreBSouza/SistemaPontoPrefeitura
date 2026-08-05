package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.domain.RegrasEfetivas;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaRepository;
import br.gov.ponto.jornada.JornadaRepository;
import br.gov.ponto.jornada.domain.Escala;
import br.gov.ponto.jornada.domain.Jornada;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.registro.domain.OrigemRegistro;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class OrgaoRegrasTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private JornadaRepository jornadaRepository;
    @Autowired
    private EscalaRepository escalaRepository;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private RegrasPontoService regrasPontoService;
    @Autowired
    private RegistroService registroService;

    private UUID tenant;
    private UUID vinculoId;
    private UUID jornadaSemIntervalo;
    private UUID orgaoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenant = tenantService.criar(new CriarTenantRequest("Ente Z", "ente-z", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "66666666666", "Zita", null,
                List.of(new CriarVinculoRequest("M-7", Regime.ESTATUTARIO, "Agente", 40))));
        vinculoId = servidor.vinculos().get(0).id();

        jornadaSemIntervalo = jornadaRepository.save(
                new Jornada(tenant, "Expediente sem intervalo", TipoJornada.FIXA, 2400, 5, 0)).getId();

        // Orgao com regras proprias; vinculo lotado herda essas regras.
        var orgao = lotacaoService.criar("Secretaria de Saúde", "SES");
        orgaoId = orgao.getId();
        lotacaoService.definirRegras(orgao.getId(), new RegrasPonto(
                jornadaSemIntervalo, 5, false,
                new BigDecimal("-16.6790000"), new BigDecimal("-49.2550000"), 150));
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoId, tenant).orElseThrow();
        v.setLotacaoId(orgao.getId());
        vinculoRepository.save(v);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void vinculoHerdaRegrasDoOrgaoEDeducaoUsaJornadaPadrao() {
        RegrasEfetivas ef = regrasPontoService.efetivasParaVinculo(vinculoId);

        assertThat(ef.comIntervalo()).isFalse();
        assertThat(ef.toleranciaMinutos()).isEqualTo(5);
        assertThat(ef.bancoHorasHabilitado()).isFalse();
        assertThat(ef.temGeofence()).isTrue();
        assertThat(ef.geofenceRaioMetros()).isEqualTo(150);
        assertThat(ef.jornadaEfetivaId()).isEqualTo(jornadaSemIntervalo);

        // Jornada do orgao sem intervalo => ciclo de 2 fases no botao unico.
        var b1 = registroService.bater(new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE, null, null, null, false, "o1"));
        var b2 = registroService.bater(new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE, null, null, null, false, "o2"));
        assertThat(b1.tipo()).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(b2.tipo()).isEqualTo(TipoMarcacao.SAIDA);
    }

    @Test
    void escalaDoVinculoSobrepoeJornadaPadraoDoOrgao() {
        UUID jornadaComIntervalo = jornadaRepository.save(
                new Jornada(tenant, "Expediente com intervalo", TipoJornada.FIXA, 2400, 10, 60)).getId();
        escalaRepository.save(new Escala(tenant, vinculoId, jornadaComIntervalo,
                LocalDate.now(TempoMunicipal.ZONE).minusDays(1), null));

        RegrasEfetivas ef = regrasPontoService.efetivasParaVinculo(vinculoId);
        assertThat(ef.jornadaEfetivaId()).isEqualTo(jornadaComIntervalo);
        assertThat(ef.comIntervalo()).isTrue();
    }

    @Test
    void baterForaDaCercaDoOrgaoEhSinalizado() {
        // O órgão da fixture tem geofence em (-16.6790, -49.2550) raio 150 m.
        var dentro = registroService.bater(new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE,
                null, new BigDecimal("-16.6790"), new BigDecimal("-49.2550"), false, "geo-dentro"));
        assertThat(dentro.foraDaCerca()).isFalse();

        var fora = registroService.bater(new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE,
                null, new BigDecimal("-23.5505"), new BigDecimal("-46.6333"), false, "geo-fora"));
        assertThat(fora.foraDaCerca()).isTrue();
    }

    @Test
    void verificacaoObrigatoriaDoOrgaoEhResolvida() {
        // Por padrão o órgão não exige verificação.
        assertThat(regrasPontoService.regrasDoOrgaoDoVinculo(vinculoId).verificacaoObrigatoria()).isFalse();

        // O admin liga a verificação obrigatória no órgão.
        lotacaoService.definirRegras(orgaoId,
                new RegrasPonto(null, null, true, null, null, null, null, true));

        assertThat(regrasPontoService.regrasDoOrgaoDoVinculo(vinculoId).verificacaoObrigatoria()).isTrue();
    }
}
