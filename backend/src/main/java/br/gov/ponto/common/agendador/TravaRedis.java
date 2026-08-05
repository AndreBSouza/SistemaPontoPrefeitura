package br.gov.ponto.common.agendador;

import br.gov.ponto.common.redis.OperacoesRedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Trava distribuída via Redis (SET NX EX): a primeira instância que grava a chave assume a rotina;
 * as demais desistem daquela execução. A chave expira sozinha, então uma instância que morra no
 * meio não deixa a tarefa travada para sempre.
 */
@Component
@Primary
@ConditionalOnBean(OperacoesRedis.class)
public class TravaRedis implements TravaDistribuida {

    private static final Logger log = LoggerFactory.getLogger(TravaRedis.class);

    private final OperacoesRedis redis;

    public TravaRedis(OperacoesRedis redis) {
        this.redis = redis;
    }

    @Override
    public boolean assumir(String tarefa, Duration duracao) {
        try {
            return redis.gravarSeAusente("trava:" + tarefa, "1", duracao);
        } catch (RuntimeException e) {
            // Redis fora do ar: não assume. Perder uma execução de rotina é melhor do que
            // disparar notificação duplicada para todos os servidores.
            log.warn("Redis indisponível ao tentar assumir a rotina {} ({}); execução pulada.",
                    tarefa, e.getClass().getSimpleName());
            return false;
        }
    }
}
