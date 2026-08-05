package br.gov.ponto.notificacao;

import br.gov.ponto.notificacao.domain.Notificacao;

/**
 * Abstracao de dispatch de notificacoes. Implementacoes reais: FCM (push) e SES (e-mail).
 */
public interface Notificador {
    void enviar(Notificacao notificacao);
}
