package br.gov.ponto.ativacao.api;

import java.util.UUID;

/**
 * Resposta da ativacao: o app guarda o {@code deviceToken} em armazenamento seguro
 * e passa a enviá-lo no cabecalho X-Device-Token (login unico por aparelho).
 */
public record AtivacaoResponse(
        String deviceToken,
        UUID tenantId,
        UUID vinculoId,
        UUID dispositivoId,
        String nomeDispositivo
) {
}
