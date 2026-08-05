package br.gov.ponto.common.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CriptografiaServiceTest {

    private final CriptografiaService cripto = new CriptografiaService("segredo-de-teste");

    @Test
    void cifraEDecifraDeVolta() {
        String original = "0.12,-0.34,0.56,0.78,-0.9";
        String cifrado = cripto.cifrar(original);
        assertThat(cifrado).isNotEqualTo(original);
        assertThat(cripto.decifrar(cifrado)).isEqualTo(original);
    }

    @Test
    void mesmoTextoGeraCifrasDiferentes() {
        // IV aleatorio por cifragem → ciphertexts distintos, ambos decifram igual.
        String a = cripto.cifrar("mesmo");
        String b = cripto.cifrar("mesmo");
        assertThat(a).isNotEqualTo(b);
        assertThat(cripto.decifrar(a)).isEqualTo("mesmo");
        assertThat(cripto.decifrar(b)).isEqualTo("mesmo");
    }
}
