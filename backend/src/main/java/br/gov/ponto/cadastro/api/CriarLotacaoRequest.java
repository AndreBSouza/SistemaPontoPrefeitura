package br.gov.ponto.cadastro.api;

import jakarta.validation.constraints.NotBlank;

public record CriarLotacaoRequest(
        @NotBlank String nome,
        String sigla
) {
}
