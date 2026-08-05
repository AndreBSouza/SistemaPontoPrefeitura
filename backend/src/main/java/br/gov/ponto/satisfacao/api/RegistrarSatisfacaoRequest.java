package br.gov.ponto.satisfacao.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Avaliação de satisfação enviada pelo servidor (nota 1..5 + comentário opcional). */
public record RegistrarSatisfacaoRequest(
        @Min(1) @Max(5) int nota,
        @Size(max = 500) String comentario
) {
}
