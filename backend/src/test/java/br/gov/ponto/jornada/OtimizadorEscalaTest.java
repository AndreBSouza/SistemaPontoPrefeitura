package br.gov.ponto.jornada;

import br.gov.ponto.jornada.domain.OtimizadorEscala;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

import static org.assertj.core.api.Assertions.assertThat;

class OtimizadorEscalaTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final BiPredicate<UUID, LocalDate> SEMPRE = (s, d) -> true;

    private static List<LocalDate> dias(int quantos) {
        return java.util.stream.IntStream.rangeClosed(1, quantos)
                .mapToObj(d -> LocalDate.of(2026, 6, d)).toList();
    }

    @Test
    void distribuiDeFormaJusta() {
        var s = OtimizadorEscala.montar(List.of(A, B), dias(4), SEMPRE, 1, 5);
        long diasA = s.escala().values().stream().filter(l -> l.contains(A)).count();
        long diasB = s.escala().values().stream().filter(l -> l.contains(B)).count();
        assertThat(diasA).isEqualTo(2);
        assertThat(diasB).isEqualTo(2);
        assertThat(s.diasDescobertos()).isEmpty();
    }

    @Test
    void respeitaTetoDeDiasConsecutivos() {
        // 1 servidor, 3 dias, teto 2 -> trabalha 2 dias e o 3º fica descoberto (folga forçada)
        var s = OtimizadorEscala.montar(List.of(A), dias(3), SEMPRE, 1, 2);
        assertThat(s.escala().get(LocalDate.of(2026, 6, 1))).containsExactly(A);
        assertThat(s.escala().get(LocalDate.of(2026, 6, 2))).containsExactly(A);
        assertThat(s.escala().get(LocalDate.of(2026, 6, 3))).isEmpty();
        assertThat(s.diasDescobertos()).containsExactly(LocalDate.of(2026, 6, 3));
    }

    @Test
    void diaSemDisponivelFicaDescoberto() {
        BiPredicate<UUID, LocalDate> soDia1 = (srv, d) -> d.getDayOfMonth() == 1;
        var s = OtimizadorEscala.montar(List.of(A), dias(2), soDia1, 1, 5);
        assertThat(s.escala().get(LocalDate.of(2026, 6, 1))).containsExactly(A);
        assertThat(s.diasDescobertos()).containsExactly(LocalDate.of(2026, 6, 2));
    }
}
