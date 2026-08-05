package br.gov.ponto.delegacao;

import br.gov.ponto.apuracao.JustificativaService;
import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.TipoJustificativa;
import br.gov.ponto.cadastro.LotacaoService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.delegacao.domain.Delegacao;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class DelegacaoTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private LotacaoService lotacaoService;
    @Autowired
    private VinculoRepository vinculoRepository;
    @Autowired
    private JustificativaService justificativaService;
    @Autowired
    private DelegacaoService delegacaoService;

    private UUID tenantId;
    private UUID gestorA;
    private UUID subB;
    private UUID vinculoC;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Del2", "ente-del2", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());

        gestorA = servidorService.criar(new CriarServidorRequest("30303030303", "Gestor A", null,
                List.of(new CriarVinculoRequest("G-A", Regime.COMISSIONADO, "Diretor", 40)))).id();
        subB = servidorService.criar(new CriarServidorRequest("40404040404", "Sub B", null,
                List.of(new CriarVinculoRequest("S-B", Regime.COMISSIONADO, "Coordenador", 40)))).id();
        var funcC = servidorService.criar(new CriarServidorRequest("50505050505", "Func C", null,
                List.of(new CriarVinculoRequest("F-C", Regime.ESTATUTARIO, "Agente", 40))));
        vinculoC = funcC.vinculos().get(0).id();

        // Órgão chefiado por A; Func C lotado nele.
        UUID orgao = lotacaoService.criar("Secretaria", "SEC").getId();
        lotacaoService.definirChefia(orgao, gestorA);
        Vinculo v = vinculoRepository.findByIdAndTenantId(vinculoC, tenantId).orElseThrow();
        v.setLotacaoId(orgao);
        vinculoRepository.save(v);

        // Func C tem uma justificativa pendente.
        justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoC, TipoJustificativa.ATESTADO,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2), "consulta", null));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void substitutoVeAsPendenciasDaChefiaDelegadaEPerdeAoRevogar() {
        // Antes: o titular A vê a pendência; o substituto B não chefia nada.
        assertThat(justificativaService.pendentesDaChefia(gestorA)).hasSize(1);
        assertThat(justificativaService.pendentesDaChefia(subB)).isEmpty();

        // A delega a aprovação para B no período vigente.
        Delegacao d = delegacaoService.criar(gestorA, subB,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));
        assertThat(delegacaoService.delegantesAtivosPara(subB)).containsExactly(gestorA);
        assertThat(justificativaService.pendentesDaChefia(subB)).hasSize(1);

        // Revogada a delegação, B deixa de ver.
        delegacaoService.revogar(d.getId());
        assertThat(delegacaoService.delegantesAtivosPara(subB)).isEmpty();
        assertThat(justificativaService.pendentesDaChefia(subB)).isEmpty();
    }

    @Test
    void delegacaoForaDaVigenciaNaoVale() {
        delegacaoService.criar(gestorA, subB,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10)); // começa no futuro
        assertThat(delegacaoService.delegantesAtivosPara(subB)).isEmpty();
        assertThat(justificativaService.pendentesDaChefia(subB)).isEmpty();
    }

    @Test
    void delegacaoRejeitaParametrosInvalidos() {
        assertThatThrownBy(() -> delegacaoService.criar(gestorA, gestorA,
                LocalDate.now(), LocalDate.now().plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> delegacaoService.criar(gestorA, subB,
                LocalDate.now().plusDays(5), LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
