package br.gov.ponto.ativacao.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** RH gera um codigo de ativacao para um vinculo (validade opcional em horas). */
public record GerarCodigoRequest(
        @NotNull UUID vinculoId,
        @Positive Integer validadeHoras
) {
}
