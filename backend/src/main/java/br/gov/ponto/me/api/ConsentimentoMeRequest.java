package br.gov.ponto.me.api;

import jakarta.validation.constraints.NotBlank;

/** Consentimento LGPD do próprio servidor (ex.: finalidade "BIOMETRIA"). */
public record ConsentimentoMeRequest(@NotBlank String finalidade, boolean concedido) {
}
