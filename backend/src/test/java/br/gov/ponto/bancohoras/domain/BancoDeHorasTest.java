package br.gov.ponto.bancohoras.domain;

import br.gov.ponto.common.error.ConflitoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

/** Teste unitario puro das invariantes do banco de horas. */
class BancoDeHorasTest {

    private static final int LIMITE = 200 * 60;

    @Test
    void creditoRespeitaOTeto() {
        assertThat(BancoDeHoras.de(0, LIMITE).aplicarLiquido(60)).isEqualTo(60);
        assertThat(BancoDeHoras.de(LIMITE - 20, LIMITE).aplicarLiquido(60)).isEqualTo(20);
        assertThat(BancoDeHoras.de(LIMITE, LIMITE).aplicarLiquido(60)).isZero();
    }

    @Test
    void debitoLiquidoPassaDireto() {
        assertThat(BancoDeHoras.de(100, LIMITE).aplicarLiquido(-30)).isEqualTo(-30);
    }

    @Test
    void compensacaoValidaSaldoEValor() {
        assertThatThrownBy(() -> BancoDeHoras.de(60, LIMITE).validarCompensacao(100))
                .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> BancoDeHoras.de(60, LIMITE).validarCompensacao(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> BancoDeHoras.de(60, LIMITE).validarCompensacao(30))
                .doesNotThrowAnyException();
    }
}
