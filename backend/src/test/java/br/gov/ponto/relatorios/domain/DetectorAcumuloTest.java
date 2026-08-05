package br.gov.ponto.relatorios.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorAcumuloTest {

    private static DetectorAcumulo.Janela janela(int dia, int horaInicio, int horaFim) {
        return new DetectorAcumulo.Janela(dia, horaInicio * 60, horaFim * 60);
    }

    @Test
    void detectaSobreposicaoNoMesmoDia() {
        List<DetectorAcumulo.Janela> a = List.of(janela(1, 8, 12));   // seg 08–12
        List<DetectorAcumulo.Janela> b = List.of(janela(1, 11, 15));  // seg 11–15 (colide 11–12)
        assertThat(DetectorAcumulo.haConflito(a, b)).isTrue();
    }

    @Test
    void semConflitoEmDiasDiferentes() {
        List<DetectorAcumulo.Janela> a = List.of(janela(1, 8, 12)); // segunda
        List<DetectorAcumulo.Janela> b = List.of(janela(2, 8, 12)); // terça
        assertThat(DetectorAcumulo.haConflito(a, b)).isFalse();
    }

    @Test
    void semConflitoQuandoHorariosNaoColidemNoMesmoDia() {
        List<DetectorAcumulo.Janela> a = List.of(janela(1, 8, 12));  // manhã
        List<DetectorAcumulo.Janela> b = List.of(janela(1, 13, 17)); // tarde
        assertThat(DetectorAcumulo.haConflito(a, b)).isFalse();
    }

    @Test
    void bordasNaoContamComoConflito() {
        // Janela semiaberta [s,e): fim 12:00 encosta no início 12:00 sem colidir.
        List<DetectorAcumulo.Janela> a = List.of(janela(1, 8, 12));
        List<DetectorAcumulo.Janela> b = List.of(janela(1, 12, 16));
        assertThat(DetectorAcumulo.haConflito(a, b)).isFalse();
    }
}
