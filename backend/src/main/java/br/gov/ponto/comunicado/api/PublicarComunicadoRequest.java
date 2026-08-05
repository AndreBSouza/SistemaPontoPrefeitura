package br.gov.ponto.comunicado.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Publicação de comunicado oficial. {@code lotacaoId} nulo = comunicado geral (todos). */
public record PublicarComunicadoRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 4000) String mensagem,
        UUID lotacaoId
) {
}
