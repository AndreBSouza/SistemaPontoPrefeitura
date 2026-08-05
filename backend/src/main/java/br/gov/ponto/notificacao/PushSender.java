package br.gov.ponto.notificacao;

/**
 * Porta de envio de push (mobile). Implementação real (a plugar): FCM (Firebase Cloud Messaging)
 * usando o projeto/credenciais do ente — registre um {@code @Component} que implemente esta porta
 * e o {@link RoteadorNotificador} passa a usá-lo para o canal PUSH. Sem ela, cai no log.
 */
public interface PushSender {
    void enviar(String destinatario, String titulo, String mensagem);
}
