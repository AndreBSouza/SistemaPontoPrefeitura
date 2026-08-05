package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.domain.DetectorAnomalia;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorAnomaliaTest {

    @Test
    void sinalizaValorMuitoAcimaDaMedia() {
        Map<String, Integer> he = new LinkedHashMap<>();
        he.put("a", 50);
        he.put("b", 60);
        he.put("c", 6000); // muito acima da média
        assertThat(DetectorAnomalia.outliers(he, 2.0, 600)).containsExactly("c");
    }

    @Test
    void naoSinalizaAmostraHomogenea() {
        Map<String, Integer> he = new LinkedHashMap<>();
        he.put("a", 700);
        he.put("b", 720);
        he.put("c", 680);
        assertThat(DetectorAnomalia.outliers(he, 2.0, 600)).isEmpty();
    }

    @Test
    void pisoEvitaFalsoPositivoEmValorPequeno() {
        // 'c' é 10× a média, mas ainda abaixo do piso de 10h -> não é anomalia relevante.
        Map<String, Integer> he = new LinkedHashMap<>();
        he.put("a", 5);
        he.put("b", 5);
        he.put("c", 100);
        assertThat(DetectorAnomalia.outliers(he, 2.0, 600)).isEmpty();
    }

    @Test
    void mapaVazioNaoQuebra() {
        assertThat(DetectorAnomalia.outliers(Map.<String, Integer>of(), 2.0, 600)).isEmpty();
    }
}
