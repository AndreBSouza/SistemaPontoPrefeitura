package br.gov.ponto.lgpd.api;

import java.util.UUID;

public record ExportacaoTitularResponse(
        UUID servidorId,
        String nome,
        String cpf,
        String email,
        int totalVinculos,
        int totalRegistros
) {
}
