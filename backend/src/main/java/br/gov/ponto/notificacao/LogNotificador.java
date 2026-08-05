package br.gov.ponto.notificacao;

import br.gov.ponto.notificacao.domain.Notificacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Dispatch padrao (fundacao): apenas registra em log. Sera substituido por
 * provedores reais (FCM para push, SES para e-mail) por canal.
 */
@Component
public class LogNotificador implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(LogNotificador.class);

    @Override
    public void enviar(Notificacao notificacao) {
        log.info("[NOTIFICACAO/{}] para {}: {}", notificacao.getCanal(),
                notificacao.getDestinatario(), notificacao.getAssunto());
    }
}
