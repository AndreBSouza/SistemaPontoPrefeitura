package br.gov.ponto.ia.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaRequest(@NotBlank @Size(max = 500) String pergunta) {
}
