package br.gov.ponto.notificacao.api;

import br.gov.ponto.notificacao.domain.CanalNotificacao;
import br.gov.ponto.notificacao.domain.Notificacao;

import java.time.Instant;
import java.util.UUID;

public record NotificacaoResponse(
        UUID id,
        String destinatario,
        String assunto,
        String mensagem,
        CanalNotificacao canal,
        Instant enviadaEm
) {
    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getDestinatario(), n.getAssunto(),
                n.getMensagem(), n.getCanal(), n.getEnviadaEm());
    }
}
