package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.api.CriarGeofenceLocalRequest;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.RegrasPonto;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.api.BaterPontoRequest;
import br.gov.ponto.registro.api.BatidaResponse;
import br.gov.ponto.registro.domain.OrigemRegistro;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class GeofenceLocalTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private GeofenceLocalService geofenceLocalService;
    @Autowired
    private RegistroService registroService;

    private UUID orgaoId;
    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenantId = tenantService.criar(
                new CriarTenantRequest("Ente G", "ente-g", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        orgaoId = lotacaoService.criar("Vigilância Sanitária", "VISA").getId();
        var servidor = servidorService.criar(new CriarServidorRequest(
                "88888888888", "Vera", null,
                List.of(new CriarVinculoRequest("M-9", Regime.ESTATUTARIO, "Fiscal", 40))));
        vinculoId = servidor.vinculos().get(0).id();
        servidorService.lotarVinculo(vinculoId, orgaoId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private BatidaResponse bater(String lat, String lng, String key) {
        return registroService.bater(new BaterPontoRequest(vinculoId, OrigemRegistro.MOBILE, null,
                new BigDecimal(lat), new BigDecimal(lng), false, key));
    }

    @Test
    void criarListarRemoverAreas() {
        var a = geofenceLocalService.criar(orgaoId,
                new CriarGeofenceLocalRequest("Posto A", new BigDecimal("-16.68"), new BigDecimal("-49.25"), 100));
        geofenceLocalService.criar(orgaoId,
                new CriarGeofenceLocalRequest("Posto B", new BigDecimal("-23.55"), new BigDecimal("-46.63"), 100));
        assertThat(geofenceLocalService.listar(orgaoId)).hasSize(2);

        geofenceLocalService.remover(orgaoId, a.id());
        assertThat(geofenceLocalService.listar(orgaoId)).extracting(r -> r.nome()).containsExactly("Posto B");
    }

    @Test
    void foraDaAreaSoQuandoForaDeTodasAsAreas() {
        // Órgão com duas áreas (locais volantes), sem cerca primária.
        geofenceLocalService.criar(orgaoId,
                new CriarGeofenceLocalRequest("Posto A", new BigDecimal("-16.6800"), new BigDecimal("-49.2500"), 100));
        geofenceLocalService.criar(orgaoId,
                new CriarGeofenceLocalRequest("Posto B", new BigDecimal("-23.5500"), new BigDecimal("-46.6300"), 100));

        assertThat(bater("-16.6801", "-49.2501", "k1").foraDaCerca()).isFalse(); // dentro de A
        assertThat(bater("-23.5501", "-46.6301", "k2").foraDaCerca()).isFalse(); // dentro de B
        assertThat(bater("0.0", "0.0", "k3").foraDaCerca()).isTrue();            // fora de ambas
    }

    @Test
    void combinaCercaPrimariaDoOrgaoComOsLocais() {
        // Cerca primária do órgão em A + um local volante em B.
        lotacaoService.definirRegras(orgaoId, new RegrasPonto(null, null, true,
                new BigDecimal("-16.6800"), new BigDecimal("-49.2500"), 100));
        geofenceLocalService.criar(orgaoId,
                new CriarGeofenceLocalRequest("Posto B", new BigDecimal("-23.5500"), new BigDecimal("-46.6300"), 100));

        assertThat(bater("-23.5501", "-46.6301", "c1").foraDaCerca()).isFalse(); // dentro do local B
        assertThat(bater("-16.6801", "-49.2501", "c2").foraDaCerca()).isFalse(); // dentro da cerca primária A
        assertThat(bater("10.0", "10.0", "c3").foraDaCerca()).isTrue();          // fora de tudo
    }

    @Test
    void areasNaoVazamEntreEntes() {
        geofenceLocalService.criar(orgaoId,
                new CriarGeofenceLocalRequest("Posto A", new BigDecimal("-16.68"), new BigDecimal("-49.25"), 100));

        // Outro ente não enxerga as áreas do primeiro.
        TenantContext.clear();
        UUID outro = tenantService.criar(
                new CriarTenantRequest("Ente H", "ente-h", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(outro.toString());
        UUID orgaoOutro = lotacaoService.criar("Outra", "OUT").getId();
        assertThat(geofenceLocalService.listar(orgaoOutro)).isEmpty();
        assertThat(geofenceLocalService.listar(orgaoId)).isEmpty(); // órgão de outro ente
    }
}
