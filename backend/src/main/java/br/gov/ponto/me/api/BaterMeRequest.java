package br.gov.ponto.me.api;

import br.gov.ponto.registro.domain.OrigemRegistro;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;

/** Batida do app: o vinculo vem do dispositivo autenticado (não do corpo). */
public record BaterMeRequest(
        OrigemRegistro origem,
        Instant dataHoraDispositivo,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean offline,
        @NotBlank String idempotencyKey
) {
}
