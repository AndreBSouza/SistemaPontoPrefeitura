package br.gov.ponto.jornada.api;

import java.time.LocalTime;

public record HorarioResponse(
        int diaSemana,
        LocalTime horaEntrada,
        LocalTime horaSaida
) {
}
