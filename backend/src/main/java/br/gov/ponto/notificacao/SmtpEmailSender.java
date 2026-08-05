package br.gov.ponto.notificacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envio de e-mail por SMTP (compatível com Amazon SES SMTP, ou qualquer servidor SMTP do ente).
 *
 * <p>Ativado apenas quando {@code spring.mail.host} está definido (o Spring Boot então
 * autoconfigura o {@link JavaMailSender}). Configure host/porta/usuário/senha em {@code spring.mail.*}
 * e o remetente em {@code notificacao.email.remetente}. Sem isso, prevalece o log.</p>
 */
@Component
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${notificacao.email.remetente:nao-responder@ponto.local}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public void enviar(String destinatario, String assunto, String mensagem) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(destinatario);
        msg.setSubject(assunto);
        msg.setText(mensagem == null ? "" : mensagem);
        mailSender.send(msg);
    }
}
