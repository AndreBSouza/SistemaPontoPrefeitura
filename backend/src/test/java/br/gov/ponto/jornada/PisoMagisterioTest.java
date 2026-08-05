package br.gov.ponto.jornada;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.PisoMagisterioResponse;
import br.gov.ponto.jornada.domain.TipoJornada;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class PisoMagisterioTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private JornadaService jornadaService;
    @Autowired
    private PisoMagisterioService pisoMagisterioService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var tenant = tenantService.criar(new CriarTenantRequest("Ente E", "ente-e", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void avaliaSomenteJornadasComHoraAtividadeESinalizaAbaixoDoMinimo() {
        // 40h = 2400 min; 1/3 = 800 min.
        jornadaService.criar(new CriarJornadaRequest("Prof OK", TipoJornada.MAGISTERIO, 2400, 0, 0, 800));
        jornadaService.criar(new CriarJornadaRequest("Prof Abaixo", TipoJornada.MAGISTERIO, 2400, 0, 0, 600));
        // Sem hora-atividade declarada → fora do relatório.
        jornadaService.criar(new CriarJornadaRequest("Admin 40h", TipoJornada.FIXA, 2400, 5, 60));

        List<PisoMagisterioResponse> avaliacao = pisoMagisterioService.avaliar();
        assertThat(avaliacao).hasSize(2);

        var ok = avaliacao.stream().filter(p -> p.nome().equals("Prof OK")).findFirst().orElseThrow();
        assertThat(ok.minimoLegalMin()).isEqualTo(800);
        assertThat(ok.atendePiso()).isTrue();

        var abaixo = avaliacao.stream().filter(p -> p.nome().equals("Prof Abaixo")).findFirst().orElseThrow();
        assertThat(abaixo.atendePiso()).isFalse();
        assertThat(abaixo.percentual()).isLessThan(1.0 / 3);
    }
}
