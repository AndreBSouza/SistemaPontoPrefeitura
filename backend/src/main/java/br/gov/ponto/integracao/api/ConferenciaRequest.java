package br.gov.ponto.integracao.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Dados da folha (o que foi pago/descontado) para conferir contra a frequência apurada. */
public record ConferenciaRequest(
        @NotEmpty List<ItemFolha> itens
) {
    public record ItemFolha(
            @NotNull UUID vinculoId,
            int horaExtraPagaMinutos,
            int faltasDescontadas
    ) {
    }
}
