package br.gov.ponto.registro.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeducaoBatidaTest {

    @Test
    void cicloComIntervaloSegueEntradaInicioFimSaida() {
        assertThat(DeducaoBatida.proximoTipo(0, true)).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(DeducaoBatida.proximoTipo(1, true)).isEqualTo(TipoMarcacao.INTERVALO_INICIO);
        assertThat(DeducaoBatida.proximoTipo(2, true)).isEqualTo(TipoMarcacao.INTERVALO_FIM);
        assertThat(DeducaoBatida.proximoTipo(3, true)).isEqualTo(TipoMarcacao.SAIDA);
    }

    @Test
    void cicloComIntervaloReiniciaAposSaida() {
        assertThat(DeducaoBatida.proximoTipo(4, true)).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(DeducaoBatida.proximoTipo(5, true)).isEqualTo(TipoMarcacao.INTERVALO_INICIO);
    }

    @Test
    void cicloSemIntervaloAlternaEntradaSaida() {
        assertThat(DeducaoBatida.proximoTipo(0, false)).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(DeducaoBatida.proximoTipo(1, false)).isEqualTo(TipoMarcacao.SAIDA);
        assertThat(DeducaoBatida.proximoTipo(2, false)).isEqualTo(TipoMarcacao.ENTRADA);
        assertThat(DeducaoBatida.proximoTipo(3, false)).isEqualTo(TipoMarcacao.SAIDA);
    }

    @Test
    void rejeitaContagemNegativa() {
        assertThatThrownBy(() -> DeducaoBatida.proximoTipo(-1, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
