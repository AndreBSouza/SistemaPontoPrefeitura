package br.gov.ponto.cadastro;

import br.gov.ponto.cadastro.api.ImportacaoResultado;
import br.gov.ponto.common.error.ConflitoException;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class ImportacaoServidorTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ImportacaoServidorService importacaoServidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private br.gov.ponto.cadastro.ServidorService servidorService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente I", "ente-i", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void importaValidosEReportaErrosPorLinha() {
        String csv = "cpf;nome;email;matricula;regime;cargo;carga\n"
                + "11111111111;Ana;ana@ente.gov.br;M1;ESTATUTARIO;Analista;40\n"
                + "11111111111;Ana Duplicada;;M2;ESTATUTARIO;;40\n"
                + "xx;Linha ruim";

        ImportacaoResultado r = importacaoServidorService.importarCsv(csv);

        assertThat(r.totalLinhas()).isEqualTo(3);
        assertThat(r.importados()).isEqualTo(1);
        assertThat(r.erros()).hasSize(2);
        assertThat(r.erros().get(0).linha()).isEqualTo(3);
    }

    @Test
    void siglaDeOrgaoEhUnicaPorEnte() {
        lotacaoService.criar("Recursos Humanos", "RH");
        assertThatThrownBy(() -> lotacaoService.criar("RH Duplicado", "RH"))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("RH");
    }

    @Test
    void importaLotandoNoOrgaoPelaColunaSiglaEPeloParametroPadrao() {
        UUID rh = lotacaoService.criar("Recursos Humanos", "RH").getId();
        UUID obras = lotacaoService.criar("Obras", "SEOB").getId();

        // 1ª linha traz a sigla "RH"; 2ª não traz coluna → usa o lotacaoId padrão (Obras).
        String csv = "22222222222;Bia;;M-1;ESTATUTARIO;Analista;40;RH\n"
                + "33333333333;Caio;;M-2;ESTATUTARIO;Auxiliar;40";
        var r = importacaoServidorService.importarCsv(csv, obras);
        assertThat(r.importados()).isEqualTo(2);

        var servidores = servidorService.listar();
        for (var s : servidores) {
            UUID vId = s.vinculos().get(0).id();
            UUID lot = vinculoRepository.findByIdAndTenantId(vId, tenantId).orElseThrow().getLotacaoId();
            assertThat(lot).isIn(rh, obras); // todos entraram lotados
        }
        // Bia → RH (coluna); Caio → Obras (padrão).
        var bia = servidores.stream().filter(x -> x.nome().equals("Bia")).findFirst().orElseThrow();
        var caio = servidores.stream().filter(x -> x.nome().equals("Caio")).findFirst().orElseThrow();
        assertThat(vinculoRepository.findByIdAndTenantId(bia.vinculos().get(0).id(), tenantId).orElseThrow().getLotacaoId()).isEqualTo(rh);
        assertThat(vinculoRepository.findByIdAndTenantId(caio.vinculos().get(0).id(), tenantId).orElseThrow().getLotacaoId()).isEqualTo(obras);
    }
}
