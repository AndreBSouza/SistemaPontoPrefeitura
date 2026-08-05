package br.gov.ponto.ativacao.api;

import java.time.Instant;
import java.util.UUID;

/** Codigo legivel (mostrado uma unica vez ao RH) para o servidor ativar o app. */
public record GerarCodigoResponse(
        String codigo,
        UUID vinculoId,
        Instant expiraEm
) {
}
