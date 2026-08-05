package br.gov.ponto.biometria.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparadorFacialTest {

    @Test
    void descritoresIdenticosTemSimilaridadeUm() {
        double[] a = {0.1, 0.2, 0.3, 0.4};
        assertThat(ComparadorFacial.similaridade(a, a)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void descritoresOrtogonaisTemSimilaridadeZero() {
        double[] a = {1, 0};
        double[] b = {0, 1};
        assertThat(ComparadorFacial.similaridade(a, b)).isCloseTo(0.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void correspondeQuandoAcimaDoLimiar() {
        double[] ref = {0.9, 0.1, 0.2};
        double[] proximo = {0.88, 0.12, 0.22};
        double[] distante = {-0.5, 0.8, -0.3};
        assertThat(ComparadorFacial.corresponde(ref, proximo, ComparadorFacial.LIMIAR_PADRAO)).isTrue();
        assertThat(ComparadorFacial.corresponde(ref, distante, ComparadorFacial.LIMIAR_PADRAO)).isFalse();
    }

    @Test
    void parseLeVetorCsv() {
        assertThat(ComparadorFacial.parse("0.1, -0.2,0.3")).containsExactly(0.1, -0.2, 0.3);
    }

    @Test
    void tamanhosDiferentesLancam() {
        assertThatThrownBy(() -> ComparadorFacial.similaridade(new double[]{1, 2}, new double[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
