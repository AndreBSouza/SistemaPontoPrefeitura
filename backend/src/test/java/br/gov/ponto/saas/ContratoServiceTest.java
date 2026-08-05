package br.gov.ponto.saas;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.saas.api.ContratoRequest;
import br.gov.ponto.saas.api.ContratoResponse;
import br.gov.ponto.saas.api.ExecucaoContratoResponse;
import br.gov.ponto.saas.domain.ModalidadeContratacao;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class ContratoServiceTest {

    @Autowired
    private TenantService tenantService;
    @Autowired
    private ContratoService contratoService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Ente C", "ente-c", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ContratoRequest req(LocalDate ini, LocalDate fim, BigDecimal mensal) {
        return new ContratoRequest(ModalidadeContratacao.PREGAO, "PE-01/2026", "2026NE000123",
                ini, fim, mensal.multiply(BigDecimal.valueOf(12)), mensal, null);
    }

    @Test
    void criaECalculaVigencia() {
        LocalDate hoje = LocalDate.now();
        contratoService.criar(req(hoje.minusMonths(1), hoje.plusMonths(6), new BigDecimal("2500.00")));
        contratoService.criar(req(hoje.minusYears(2), hoje.minusYears(1), new BigDecimal("2000.00")));

        List<ContratoResponse> lista = contratoService.listar();
        assertThat(lista).hasSize(2);
        // o mais recente primeiro (vigência início desc) e vigente
        assertThat(lista.get(0).vigente()).isTrue();
        assertThat(lista.get(1).vigente()).isFalse();
        assertThat(lista.get(0).modalidadeRotulo()).isEqualTo("Pregão eletrônico");
    }

    @Test
    void execucaoTrazParcelaDoContratoVigente() {
        YearMonth comp = YearMonth.now();
        contratoService.criar(req(comp.atDay(1).minusMonths(1), comp.atEndOfMonth().plusMonths(1),
                new BigDecimal("3000.00")));

        ExecucaoContratoResponse exec = contratoService.execucao(comp);
        assertThat(exec.contratoVigente()).isTrue();
        assertThat(exec.valorMensal()).isEqualByComparingTo("3000.00");
        assertThat(exec.numeroProcesso()).isEqualTo("PE-01/2026");
    }

    @Test
    void vigenciaInvertidaFalha() {
        LocalDate hoje = LocalDate.now();
        assertThatThrownBy(() -> contratoService.criar(req(hoje, hoje.minusDays(1), BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
