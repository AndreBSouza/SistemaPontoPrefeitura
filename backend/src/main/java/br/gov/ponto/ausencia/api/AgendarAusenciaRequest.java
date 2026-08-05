package br.gov.ponto.ausencia.api;

import br.gov.ponto.ausencia.domain.TipoAusencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AgendarAusenciaRequest(
        @NotNull UUID vinculoId,
        @NotNull TipoAusencia tipo,
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        @Size(max = 300) String observacao
) {
}
