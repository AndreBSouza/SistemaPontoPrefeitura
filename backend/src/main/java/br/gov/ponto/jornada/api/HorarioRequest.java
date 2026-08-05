package br.gov.ponto.jornada.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record HorarioRequest(
        @Min(1) @Max(7) int diaSemana,
        @NotNull LocalTime horaEntrada,
        @NotNull LocalTime horaSaida
) {
}
