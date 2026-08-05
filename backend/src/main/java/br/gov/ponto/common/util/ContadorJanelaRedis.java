package br.gov.ponto.common.util;

import br.gov.ponto.common.redis.OperacoesRedis;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Contador de janela fixa compartilhado por TODAS as instâncias (Redis).
 *
 * <p>Padrão INCR + EXPIRE: a primeira ocorrência cria a chave e define o TTL da janela; as demais
 * apenas incrementam. Quando a chave expira, a janela recomeça. Assim o limite anti-abuso vale
 * para o sistema inteiro, não por réplica.</p>
 *
 * <p>Só é registrado quando há Redis configurado ({@link OperacoesRedis} presente); caso contrário
 * prevalece o {@link ContadorJanelaMemoria}.</p>
 */
@Component
@Primary
@ConditionalOnBean(OperacoesRedis.class)
public class ContadorJanelaRedis implements ContadorJanela {

    private final OperacoesRedis redis;

    public ContadorJanelaRedis(OperacoesRedis redis) {
        this.redis = redis;
    }

    @Override
    public long registrar(String chave, Duration janela) {
        long total = redis.incrementar(chave);
        if (total == 1L) {
            // Primeira ocorrência da janela: define quando ela termina.
            redis.expirar(chave, janela);
        }
        return total;
    }
}
