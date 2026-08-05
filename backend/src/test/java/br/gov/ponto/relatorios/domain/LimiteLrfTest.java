package br.gov.ponto.relatorios.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteLrfTest {

    @Test
    void percentualSobreRclComAcrescimo() {
        BigDecimal p = LimiteLrf.percentualSobreRcl(
                new BigDecimal("50000000"), new BigDecimal("30000"), new BigDecimal("100000000"));
        assertThat(p).isEqualByComparingTo("50.03");
    }

    @Test
    void rclZeroOuNulaRetornaZero() {
        assertThat(LimiteLrf.percentualSobreRcl(new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualByComparingTo("0");
        assertThat(LimiteLrf.percentualSobreRcl(new BigDecimal("100"), BigDecimal.ZERO, null))
                .isEqualByComparingTo("0");
    }

    @Test
    void classificaRiscoNasFaixasDaLrf() {
        // Limite 54,00; prudencial 51,30; alerta a partir de 49,30.
        assertThat(LimiteLrf.classificar(new BigDecimal("40.00"))).isEqualTo(LimiteLrf.Risco.OK);
        assertThat(LimiteLrf.classificar(new BigDecimal("50.00"))).isEqualTo(LimiteLrf.Risco.ALERTA);
        assertThat(LimiteLrf.classificar(new BigDecimal("52.00"))).isEqualTo(LimiteLrf.Risco.PRUDENCIAL);
        assertThat(LimiteLrf.classificar(new BigDecimal("54.00"))).isEqualTo(LimiteLrf.Risco.PRUDENCIAL);
        assertThat(LimiteLrf.classificar(new BigDecimal("55.00"))).isEqualTo(LimiteLrf.Risco.ESTOURO);
    }
}
