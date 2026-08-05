package br.gov.ponto.relatorios;

import br.gov.ponto.bancohoras.BancoHorasService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.CriarVinculoRequest;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.me.MeService;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;
import br.gov.ponto.relatorios.api.AlertasResponse;
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
class AlertaRiscoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final YearMonth COMPETENCIA = YearMonth.of(2026, 3);
    private static final LocalDate DATA = LocalDate.of(2026, 3, 10);

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ServidorService servidorService;
    @Autowired
    private BancoHorasService bancoHorasService;
    @Autowired
    private RegistroPontoRepository registroRepository;
    @Autowired
    private AlertaRiscoService alertaRiscoService;
    @Autowired
    private MeService meService;

    private UUID tenantId;
    private UUID vinculoId;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        tenantId = tenantService.criar(new CriarTenantRequest("Ente Risk", "ente-risk", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(tenantId.toString());
        var servidor = servidorService.criar(new CriarServidorRequest(
                "66666666666", "Téo", null,
                List.of(new CriarVinculoRequest("M-1", Regime.ESTATUTARIO, "Fiscal", 40))));
        vinculoId = servidor.vinculos().get(0).id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void alertaApontaAjusteManualEBatidaForaDaCerca() {
        // Ajuste manual de banco de horas (+120) — vetor de risco a destacar.
        bancoHorasService.ajustar(vinculoId, DATA, 120, "Ajuste manual do gestor");
        // Batida marcada fora da cerca.
        Instant instante = DATA.atTime(8, 0).atZone(ZONE).toInstant();
        registroRepository.save(new RegistroPonto(tenantId, vinculoId, 1L, TipoMarcacao.ENTRADA,
                OrigemRegistro.MOBILE, instante, instante, null, null, false,
                UUID.randomUUID().toString(), true));

        AlertasResponse alertas = alertaRiscoService.alertas(COMPETENCIA);
        assertThat(alertas.ajustesManuais()).hasSize(1);
        assertThat(alertas.ajustesManuais().get(0).minutos()).isEqualTo(120);
        assertThat(alertas.ajustesManuais().get(0).vinculoId()).isEqualTo(vinculoId);
        assertThat(alertas.batidasForaDaCerca()).hasSize(1);
        assertThat(alertas.batidasForaDaCerca().get(0).vinculoId()).isEqualTo(vinculoId);
        assertThat(alertas.batidasForaDaCerca().get(0).quantidade()).isEqualTo(1);
    }

    @Test
    void resumoRefleteSaldoDeBancoDeHoras() {
        bancoHorasService.ajustar(vinculoId, DATA, 90, "Crédito manual");
        var resumo = meService.resumo(vinculoId);
        assertThat(resumo.saldoBancoHorasMinutos()).isEqualTo(90);
        assertThat(resumo.horaExtraSemanaMinutos()).isGreaterThanOrEqualTo(0);
    }
}
