package br.gov.ponto.sobreaviso;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.integracao.FolhaService;
import br.gov.ponto.sobreaviso.api.RegistrarSobreavisoRequest;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class SobreavisoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private SobreavisoService sobreavisoService;
    @Autowired
    private FolhaService folhaService;

    private UUID vinculoId;
    private String matricula;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente S", "ente-s", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        matricula = "M-SA";
        var servidor = servidorService.criar(new CriarServidorRequest(
                "60000000000", "Sandra", null,
                List.of(new CriarVinculoRequest(matricula, Regime.ESTATUTARIO, "Plantonista", 40))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void somaPorCompetenciaEListaDecrescente() {
        sobreavisoService.registrar(new RegistrarSobreavisoRequest(vinculoId, LocalDate.of(2026, 3, 5), 120, "fim de semana"));
        sobreavisoService.registrar(new RegistrarSobreavisoRequest(vinculoId, LocalDate.of(2026, 3, 10), 180, null));
        sobreavisoService.registrar(new RegistrarSobreavisoRequest(vinculoId, LocalDate.of(2026, 4, 2), 60, null));

        assertThat(sobreavisoService.totalMinutos(vinculoId, YearMonth.of(2026, 3))).isEqualTo(300);
        assertThat(sobreavisoService.totalMinutos(vinculoId, YearMonth.of(2026, 4))).isEqualTo(60);

        var lista = sobreavisoService.listarPorVinculo(vinculoId);
        assertThat(lista).hasSize(3);
        assertThat(lista.get(0).data()).isEqualTo(LocalDate.of(2026, 4, 2)); // mais recente primeiro
    }

    @Test
    void removerExcluiOSobreaviso() {
        var s = sobreavisoService.registrar(
                new RegistrarSobreavisoRequest(vinculoId, LocalDate.of(2026, 3, 5), 120, null));
        sobreavisoService.remover(s.id());
        assertThat(sobreavisoService.listarPorVinculo(vinculoId)).isEmpty();
    }

    @Test
    void folhaExportaMinutosDeSobreaviso() {
        sobreavisoService.registrar(
                new RegistrarSobreavisoRequest(vinculoId, LocalDate.of(2026, 3, 8), 200, null));

        String csv = folhaService.exportarCsv(YearMonth.of(2026, 3));
        assertThat(csv).contains("sobreavisoMin"); // cabeçalho
        // A linha do vínculo termina com a coluna de sobreaviso = 200.
        String linha = csv.lines().filter(l -> l.startsWith(matricula + ";")).findFirst().orElseThrow();
        assertThat(linha).endsWith(";200");
    }
}
