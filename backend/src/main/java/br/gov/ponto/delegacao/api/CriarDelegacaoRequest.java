package br.gov.ponto.delegacao.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CriarDelegacaoRequest(
        @NotNull UUID deleganteServidorId,
        @NotNull UUID delegadoServidorId,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim
) {
}
