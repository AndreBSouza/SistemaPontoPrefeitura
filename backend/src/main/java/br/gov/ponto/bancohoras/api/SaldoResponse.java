package br.gov.ponto.bancohoras.api;

import java.util.UUID;

public record SaldoResponse(
        UUID vinculoId,
        int saldoMinutos
) {
}
