package br.gov.ponto.me.api;

import jakarta.validation.constraints.NotBlank;

/** Descritor facial (embedding em CSV) capturado no aparelho para verificacao 1:1. */
public record VerificarFaceRequest(
        @NotBlank String descritor
) {
}
