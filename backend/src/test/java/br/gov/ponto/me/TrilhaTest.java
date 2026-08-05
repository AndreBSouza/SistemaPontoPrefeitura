package br.gov.ponto.me;

import br.gov.ponto.apuracao.JustificativaService;
import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.TipoJustificativa;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.correcao.CorrecaoService;
import br.gov.ponto.me.api.TrilhaEvento;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class TrilhaTest {

    private static final LocalDate DATA = LocalDate.of(2026, 3, 2);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private JustificativaService justificativaService;
    @Autowired
    private CorrecaoService correcaoService;
    @Autowired
    private TrilhaService trilhaService;

    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(
                new CriarTenantRequest("Ente T", "ente-t", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());
        var servidor = servidorService.criar(new CriarServidorRequest(
                "99999999999", "Tom", null,
                List.of(new CriarVinculoRequest("M-T", Regime.ESTATUTARIO, "Agente", 40))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void trilhaAgregaCorrecoesEJustificativasEmOrdemDecrescente() {
        var j = justificativaService.solicitar(new SolicitarJustificativaRequest(
                vinculoId, TipoJustificativa.ATESTADO, DATA, DATA, "consulta médica", null));
        justificativaService.aprovar(j.id(), "deferido");

        var c = correcaoService.solicitar(vinculoId,
                Instant.parse("2026-03-02T11:00:00Z"), TipoMarcacao.ENTRADA, "esqueci de bater");
        correcaoService.aprovar(c.getId(), "ok");

        List<TrilhaEvento> trilha = trilhaService.montar(vinculoId);

        // 2 da justificativa (solicitada + aprovada) + 2 da correção (solicitada + aprovada).
        assertThat(trilha).hasSize(4);
        assertThat(trilha).extracting(TrilhaEvento::categoria)
                .contains("CORRECAO", "JUSTIFICATIVA");
        assertThat(trilha).extracting(TrilhaEvento::titulo)
                .anyMatch(t -> t.contains("aprovada"));
        // Ordenada do mais recente para o mais antigo.
        assertThat(trilha).isSortedAccordingTo(
                Comparator.comparing(TrilhaEvento::instante).reversed());
    }

    @Test
    void trilhaVaziaQuandoSemEventos() {
        assertThat(trilhaService.montar(vinculoId)).isEmpty();
    }
}
