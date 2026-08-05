package br.gov.ponto.lgpd.api;

import java.util.UUID;

public record EliminacaoResponse(
        UUID servidorId,
        String status,
        String mensagem
) {
}
