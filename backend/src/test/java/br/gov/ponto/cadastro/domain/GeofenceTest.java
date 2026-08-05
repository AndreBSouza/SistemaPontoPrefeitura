package br.gov.ponto.cadastro.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeofenceTest {

    // Centro: Praça Cívica, Goiânia-GO.
    private final Geofence cerca = new Geofence(-16.6790, -49.2550, 150);

    @Test
    void pontoNoCentroEstaDentro() {
        assertThat(cerca.contem(-16.6790, -49.2550)).isTrue();
        assertThat(cerca.distanciaMetros(-16.6790, -49.2550)).isLessThan(1.0);
    }

    @Test
    void pontoProximoDentroDoRaio() {
        // ~100 m ao norte (0.0009° de latitude) — dentro do raio de 150 m.
        assertThat(cerca.contem(-16.6781, -49.2550)).isTrue();
    }

    @Test
    void pontoLogoAlemDoRaioEstaFora() {
        // ~222 m ao norte (0.0020° de latitude) — além dos 150 m.
        assertThat(cerca.contem(-16.6770, -49.2550)).isFalse();
    }

    @Test
    void pontoDistanteEstaFora() {
        // São Paulo-SP, muito longe de Goiânia.
        assertThat(cerca.contem(-23.5505, -46.6333)).isFalse();
    }
}
