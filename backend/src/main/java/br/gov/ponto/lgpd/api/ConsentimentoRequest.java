package br.gov.ponto.lgpd.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConsentimentoRequest(
        @NotNull UUID servidorId,
        @NotBlank String finalidade,
        boolean concedido
) {
}
