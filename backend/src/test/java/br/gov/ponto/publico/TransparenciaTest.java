package br.gov.ponto.publico;

import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.publico.api.TransparenciaResponse;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
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
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class TransparenciaTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DATA = LocalDate.of(2026, 3, 2);
    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private RegistroPontoRepository registroRepository;
    @Autowired
    private TransparenciaService transparenciaService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        UUID tenantId = tenantService.criar(
                new CriarTenantRequest("Município Transparente", "municipio-x", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var s = servidorService.criar(new CriarServidorRequest("17171717171", "Ed", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Agente", 40))));
        UUID vinculoId = s.vinculos().get(0).id();
        Instant instante = DATA.atTime(8, 0).atZone(ZONE).toInstant();
        registroRepository.save(new RegistroPonto(tenantId, vinculoId, 1L, TipoMarcacao.ENTRADA,
                OrigemRegistro.MOBILE, instante, instante, null, null, false, UUID.randomUUID().toString()));
        TenantContext.clear(); // o serviço público resolve o ente pelo slug, sem contexto prévio
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void transparenciaAgregaSemContextoEporSlug() {
        TransparenciaResponse r = transparenciaService.publico("municipio-x", COMPETENCIA);
        assertThat(r.ente()).isEqualTo("Município Transparente");
        assertThat(r.totalServidores()).isEqualTo(1);
        assertThat(r.totalVinculos()).isEqualTo(1);
        assertThat(r.totalRegistros()).isEqualTo(1);
    }
}
