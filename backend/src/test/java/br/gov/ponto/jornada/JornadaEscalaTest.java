package br.gov.ponto.jornada;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class JornadaEscalaTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private JornadaService jornadaService;
    @Autowired
    private EscalaService escalaService;

    private UUID vinculoId;
    private UUID jornadaId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenant = tenantService.criar(
                new CriarTenantRequest("Ente X", "ente-x", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenant.toString());

        var servidor = servidorService.criar(new CriarServidorRequest(
                "44444444444", "Dora", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Analista", 40))));
        vinculoId = servidor.vinculos().get(0).id();

        jornadaId = jornadaService.criar(
                new CriarJornadaRequest("40h semanais", TipoJornada.FIXA, 2400, 5, 60)).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void rejeitaJornadaComNomeDuplicado() {
        assertThatThrownBy(() -> jornadaService.criar(
                new CriarJornadaRequest("40h semanais", TipoJornada.FIXA, 2400, 5, 60)))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void rejeitaEscalaSobrepostaEPermiteVigenciaSeguinte() {
        escalaService.atribuir(new CriarEscalaRequest(
                vinculoId, jornadaId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)));

        // sobreposicao com a vigencia existente -> conflito
        assertThatThrownBy(() -> escalaService.atribuir(new CriarEscalaRequest(
                vinculoId, jornadaId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31))))
                .isInstanceOf(ConflitoException.class);

        // vigencia seguinte (sem sobreposicao) -> permitida
        assertThatCode(() -> escalaService.atribuir(new CriarEscalaRequest(
                vinculoId, jornadaId, LocalDate.of(2026, 7, 1), null)))
                .doesNotThrowAnyException();
    }

    @Test
    void aplicaJornadaEmMassaPulandoVinculoComVigenciaSobreposta() {
        // Segundo vínculo sem escala; primeiro já com escala vigente que vai sobrepor.
        var servidor2 = servidorService.criar(new CriarServidorRequest(
                "55555555555", "Eva", null,
                List.of(new CriarVinculoRequest("M-3", Regime.ESTATUTARIO, "Técnica", 40))));
        UUID vinculo2 = servidor2.vinculos().get(0).id();
        escalaService.atribuir(new CriarEscalaRequest(vinculoId, jornadaId, LocalDate.of(2026, 1, 1), null));

        // Aplica a jornada (template) aos dois: vinculoId é pulado (sobreposição), vinculo2 recebe.
        var aplicadas = escalaService.atribuirEmLote(jornadaId, List.of(vinculoId, vinculo2),
                LocalDate.of(2026, 1, 1), null);
        assertThat(aplicadas).hasSize(1);
        assertThat(aplicadas.get(0).vinculoId()).isEqualTo(vinculo2);
        assertThat(escalaService.listarPorVinculo(vinculo2)).hasSize(1);
    }

    @Test
    void trocaTurnoEntreDoisVinculos() {
        UUID jornadaB = jornadaService.criar(
                new CriarJornadaRequest("Tarde 13-17", TipoJornada.FIXA, 1200, 5, 0)).id();

        var escalaA = escalaService.atribuir(new CriarEscalaRequest(
                vinculoId, jornadaId, LocalDate.of(2026, 1, 1), null));

        var servidor2 = servidorService.criar(new CriarServidorRequest(
                "88888888888", "Bia", null,
                List.of(new CriarVinculoRequest("M-2", Regime.ESTATUTARIO, "Auxiliar", 40))));
        UUID vinculo2 = servidor2.vinculos().get(0).id();
        var escalaB = escalaService.atribuir(new CriarEscalaRequest(
                vinculo2, jornadaB, LocalDate.of(2026, 1, 1), null));

        escalaService.trocarTurno(escalaA.id(), escalaB.id());

        assertThat(escalaService.listarPorVinculo(vinculoId).get(0).jornadaId()).isEqualTo(jornadaB);
        assertThat(escalaService.listarPorVinculo(vinculo2).get(0).jornadaId()).isEqualTo(jornadaId);
    }
}
