package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.domain.ProjecaoAbsenteismo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjecaoAbsenteismoTest {

    @Test
    void serieCrescenteProjetaAcimaDaMedia() {
        // 10, 20, 30 -> média 20, tendência +10 -> projeta 30
        assertThat(ProjecaoAbsenteismo.projetar(List.of(10, 20, 30))).isEqualTo(30.0);
        assertThat(ProjecaoAbsenteismo.rotuloTendencia(List.of(10, 20, 30), 0.5)).isEqualTo("subindo");
    }

    @Test
    void serieEstavelProjetaAMedia() {
        assertThat(ProjecaoAbsenteismo.projetar(List.of(20, 20, 20))).isEqualTo(20.0);
        assertThat(ProjecaoAbsenteismo.rotuloTendencia(List.of(20, 20, 20), 0.5)).isEqualTo("estável");
    }

    @Test
    void nuncaProjetaNegativo() {
        // queda forte: 30, 10, 0 -> média ~13,3, tendência -15 -> clamp em 0
        assertThat(ProjecaoAbsenteismo.projetar(List.of(30, 10, 0))).isEqualTo(0.0);
        assertThat(ProjecaoAbsenteismo.rotuloTendencia(List.of(30, 10, 0), 0.5)).isEqualTo("caindo");
    }

    @Test
    void serieVaziaEhZero() {
        assertThat(ProjecaoAbsenteismo.projetar(List.of())).isEqualTo(0.0);
    }
}
