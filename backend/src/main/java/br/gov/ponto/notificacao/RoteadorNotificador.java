package br.gov.ponto.notificacao;

import br.gov.ponto.notificacao.domain.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dispatch principal ({@code @Primary}): roteia a notificação pelo canal para o adaptador real
 * ({@link EmailSender}/{@link PushSender}), quando presente. Sem adaptador para o canal — ou em
 * caso de falha no envio — cai no {@link LogNotificador} (comportamento atual, sem regressão).
 *
 * <p>Plugar um provedor = registrar um {@code @Component} de {@link EmailSender} (já há o
 * {@link SmtpEmailSender}, ativado por {@code spring.mail.host}) ou de {@link PushSender} (FCM).</p>
 */
@Component
@Primary
public class RoteadorNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(RoteadorNotificador.class);

    private final List<EmailSender> emailSenders;
    private final List<PushSender> pushSenders;
    private final LogNotificador fallback;

    public RoteadorNotificador(List<EmailSender> emailSenders,
                               List<PushSender> pushSenders,
                               LogNotificador fallback) {
        this.emailSenders = emailSenders;
        this.pushSenders = pushSenders;
        this.fallback = fallback;
    }

    @Override
    public void enviar(Notificacao n) {
        try {
            switch (n.getCanal()) {
                case EMAIL -> {
                    if (!emailSenders.isEmpty()) {
                        emailSenders.get(0).enviar(n.getDestinatario(), n.getAssunto(), n.getMensagem());
                        return;
                    }
                }
                case PUSH -> {
                    if (!pushSenders.isEmpty()) {
                        pushSenders.get(0).enviar(n.getDestinatario(), n.getAssunto(), n.getMensagem());
                        return;
                    }
                }
            }
            // Sem adaptador para o canal: registra em log (entrega in-app já foi persistida à parte).
            fallback.enviar(n);
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar notificação pelo canal {} ({}); registrando em log.",
                    n.getCanal(), e.getClass().getSimpleName());
            fallback.enviar(n);
        }
    }
}
