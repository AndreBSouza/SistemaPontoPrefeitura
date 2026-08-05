package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.api.SimulacaoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimuladorServiceTest {

    private final SimuladorService simulador = new SimuladorService();

    @Test
    void simulaCustoDeHoraExtraEPercentualDaRcl() {
        SimulacaoResponse r = simulador.simular(
                new BigDecimal("1000"),       // horas extras
                new BigDecimal("30"),         // custo por hora
                new BigDecimal("50000000"),   // gasto com pessoal atual
                new BigDecimal("100000000")); // RCL

        assertThat(r.custoHoraExtraReais()).isEqualByComparingTo("30000.00");
        assertThat(r.gastoProjetadoReais()).isEqualByComparingTo("50030000.00");
        assertThat(r.percentualRclProjetado()).isEqualByComparingTo("50.03");
        assertThat(r.risco()).isEqualTo("ALERTA");
        assertThat(r.limitePercentual()).isEqualByComparingTo("54.00");
        assertThat(r.prudencialPercentual()).isEqualByComparingTo("51.30");
    }

    @Test
    void rclZeradaNaoDividePorZero() {
        SimulacaoResponse r = simulador.simular(
                new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("100"), BigDecimal.ZERO);
        assertThat(r.percentualRclProjetado()).isEqualByComparingTo("0");
        assertThat(r.risco()).isEqualTo("OK");
    }
}
