package br.gov.ponto.comunicado;

import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.comunicado.domain.Comunicado;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class ComunicadoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private ComunicadoService comunicadoService;

    private UUID tenantId;
    private UUID orgaoA;
    private UUID orgaoB;
    private UUID vinculoEmA;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Com", "ente-com", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        orgaoA = lotacaoService.criar("Secretaria A", "SEA").getId();
        orgaoB = lotacaoService.criar("Secretaria B", "SEB").getId();

        var servidor = servidorService.criar(new CriarServidorRequest(
                "44444444444", "Carla", null,
                List.of(new CriarVinculoRequest("C-1", Regime.ESTATUTARIO, "Analista", 40))));
        vinculoEmA = servidor.vinculos().get(0).id();
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoEmA, tenantId).orElseThrow();
        v.setLotacaoId(orgaoA);
        vinculoRepository.save(v);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void servidorVeComunicadoGeralEDoSeuOrgaoMasNaoDeOutro() {
        comunicadoService.publicar("Recesso de fim de ano", "Expediente reduzido.", null); // geral
        comunicadoService.publicar("Reunião da Secretaria A", "Quinta às 14h.", orgaoA);    // órgão do vínculo
        comunicadoService.publicar("Mutirão da Secretaria B", "Sábado.", orgaoB);           // outro órgão

        assertThat(comunicadoService.listarTodos()).hasSize(3);

        List<Comunicado> visiveis = comunicadoService.listarParaVinculo(vinculoEmA);
        assertThat(visiveis).extracting(Comunicado::getTitulo)
                .containsExactlyInAnyOrder("Recesso de fim de ano", "Reunião da Secretaria A");
    }
}
