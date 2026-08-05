package br.gov.ponto.registro.api;

import br.gov.ponto.registro.domain.OrigemRegistro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Requisicao do "botao unico": o cliente NAO envia o tipo — o servidor deduz
 * (entrada/intervalo/saida) pela sequencia do dia + a jornada do orgao.
 */
public record BaterPontoRequest(
        @NotNull UUID vinculoId,
        @NotNull OrigemRegistro origem,
        Instant dataHoraDispositivo,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean offline,
        @NotBlank String idempotencyKey
) {
}
