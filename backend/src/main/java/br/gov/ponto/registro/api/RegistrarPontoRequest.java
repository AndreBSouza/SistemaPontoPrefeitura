package br.gov.ponto.registro.api;

import br.gov.ponto.registro.domain.OrigemRegistro;
import br.gov.ponto.registro.domain.TipoMarcacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegistrarPontoRequest(
        @NotNull UUID vinculoId,
        @NotNull TipoMarcacao tipo,
        @NotNull OrigemRegistro origem,
        Instant dataHoraDispositivo,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean offline,
        @NotBlank String idempotencyKey
) {
}
