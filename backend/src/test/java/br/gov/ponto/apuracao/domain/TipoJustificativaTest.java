package br.gov.ponto.apuracao.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Teste unitario puro do mapeamento abono -> ocorrencia neutralizada. */
class TipoJustificativaTest {

    @Test
    void atestadoNeutralizaFalta() {
        assertThat(TipoJustificativa.ATESTADO.neutraliza(TipoOcorrencia.FALTA)).isTrue();
    }

    @Test
    void atrasoNeutralizaApenasAtraso() {
        assertThat(TipoJustificativa.ATRASO.neutraliza(TipoOcorrencia.ATRASO)).isTrue();
        assertThat(TipoJustificativa.ATRASO.neutraliza(TipoOcorrencia.FALTA)).isFalse();
    }

    @Test
    void outroNaoNeutralizaNada() {
        assertThat(TipoJustificativa.OUTRO.neutraliza(TipoOcorrencia.FALTA)).isFalse();
    }
}
