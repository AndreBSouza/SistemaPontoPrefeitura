package br.gov.ponto.relatorios;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.relatorios.api.AdesaoResponse;
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
class AdesaoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private AtivacaoService ativacaoService;
    @Autowired
    private AdesaoService adesaoService;

    private UUID tenantId;
    private UUID orgaoA;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Ade", "ente-ade", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        orgaoA = lotacaoService.criar("Gabinete", "GAB").getId();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private UUID criarVinculoLotado(String cpf, String nome, String matricula, Regime regime, UUID orgao) {
        var s = servidorService.criar(new CriarServidorRequest(cpf, nome, null,
                List.of(new CriarVinculoRequest(matricula, regime, "Cargo", 40))));
        UUID v = s.vinculos().get(0).id();
        Vinculo vinc = vinculoRepository.findByIdAndTenantId(v, tenantId).orElseThrow();
        vinc.setLotacaoId(orgao);
        vinculoRepository.save(vinc);
        return v;
    }

    private void aderir(UUID vinculoId) {
        var codigo = ativacaoService.gerar(vinculoId, null);
        ativacaoService.ativar(codigo.codigo(), "Aparelho");
    }

    @Test
    void adesaoPorOrgaoEPorRegimeContamVinculosComDispositivoAtivo() {
        UUID orgaoB = lotacaoService.criar("Secretaria", "SEC").getId();
        UUID ana = criarVinculoLotado("11111111111", "Ana", "A-1", Regime.COMISSIONADO, orgaoA);
        criarVinculoLotado("22222222222", "Bia", "B-1", Regime.ESTATUTARIO, orgaoA);
        criarVinculoLotado("33333333333", "Cid", "C-1", Regime.COMISSIONADO, orgaoB);
        aderir(ana); // só a Ana ativou o app

        AdesaoResponse porOrgao = adesaoService.porOrgao();
        AdesaoResponse.Grupo gabinete = porOrgao.grupos().stream()
                .filter(g -> g.chave().equals(orgaoA.toString())).findFirst().orElseThrow();
        assertThat(gabinete.vinculos()).isEqualTo(2);
        assertThat(gabinete.aderiram()).isEqualTo(1);
        assertThat(gabinete.percentual()).isEqualTo(50);

        AdesaoResponse porRegime = adesaoService.porRegime();
        AdesaoResponse.Grupo comissionados = porRegime.grupos().stream()
                .filter(g -> g.chave().equals("COMISSIONADO")).findFirst().orElseThrow();
        assertThat(comissionados.vinculos()).isEqualTo(2);   // Ana + Cid
        assertThat(comissionados.aderiram()).isEqualTo(1);   // só Ana
        assertThat(comissionados.percentual()).isEqualTo(50);
    }
}
