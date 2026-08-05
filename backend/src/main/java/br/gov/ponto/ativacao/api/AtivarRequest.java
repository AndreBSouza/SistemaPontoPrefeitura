package br.gov.ponto.ativacao.api;

import jakarta.validation.constraints.NotBlank;

/** App troca o codigo (digitado pelo servidor) por um token de dispositivo. */
public record AtivarRequest(
        @NotBlank String codigo,
        String nomeDispositivo
) {
}
