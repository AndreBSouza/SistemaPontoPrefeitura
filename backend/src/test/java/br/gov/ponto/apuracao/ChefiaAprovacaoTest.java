package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.TipoJustificativa;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class ChefiaAprovacaoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private JustificativaService justificativaService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(new CriarTenantRequest("Ente H", "ente-h", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void chefiaEnxergaPendentesDaSuaLotacao() {
        UUID chefiaId = servidorService.criar(new CriarServidorRequest(
                "11111111111", "Chefe", null,
                List.of(new CriarVinculoRequest("M-CHEFE", Regime.COMISSIONADO, "Diretor", 40)))).id();

        UUID lotacaoId = lotacaoService.criar("Secretaria X", "SX").getId();
        lotacaoService.definirChefia(lotacaoId, chefiaId);

        var subordinado = servidorService.criar(new CriarServidorRequest(
                "22222222222", "Subordinado", null,
                List.of(new CriarVinculoRequest("M-SUB", Regime.ESTATUTARIO, "Auxiliar", 40))));
        UUID vinculoId = subordinado.vinculos().get(0).id();
        servidorService.lotarVinculo(vinculoId, lotacaoId);

        justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10), "consulta", null));

        assertThat(justificativaService.pendentesDaChefia(chefiaId)).hasSize(1);
        // chefia inexistente nao ve nada
        assertThat(justificativaService.pendentesDaChefia(UUID.randomUUID())).isEmpty();
    }
}
