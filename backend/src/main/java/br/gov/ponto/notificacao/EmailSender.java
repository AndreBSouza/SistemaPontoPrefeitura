package br.gov.ponto.notificacao;

/**
 * Porta de envio de e-mail. Implementação real: {@link SmtpEmailSender} (SMTP/SES), ativada quando
 * {@code spring.mail.host} está configurado. Sem ela, o {@link RoteadorNotificador} cai no log.
 */
public interface EmailSender {
    void enviar(String destinatario, String assunto, String mensagem);
}
