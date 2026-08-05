package br.gov.ponto.jornada.api;

import br.gov.ponto.jornada.domain.TipoJornada;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CriarJornadaRequest(
        @NotBlank String nome,
        @NotNull TipoJornada tipo,
        @Positive int cargaHorariaSemanalMin,
        @PositiveOrZero int toleranciaMin,
        @PositiveOrZero int intervaloMin,
        @PositiveOrZero Integer horaAtividadeMin
) {
    /** Compatibilidade: jornada sem hora-atividade definida. */
    public CriarJornadaRequest(String nome, TipoJornada tipo, int cargaHorariaSemanalMin,
                               int toleranciaMin, int intervaloMin) {
        this(nome, tipo, cargaHorariaSemanalMin, toleranciaMin, intervaloMin, null);
    }
}
