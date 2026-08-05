package br.gov.ponto.common.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Implementação real sobre o Redis (Lettuce/Spring Data). Registrada apenas quando
 * {@code spring.data.redis.host} está configurado — sem isso o sistema roda tudo em memória.
 */
@Component
@ConditionalOnProperty(prefix = "spring.data.redis", name = "host")
public class OperacoesRedisSpring implements OperacoesRedis {

    private final StringRedisTemplate redis;

    public OperacoesRedisSpring(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long incrementar(String chave) {
        Long valor = redis.opsForValue().increment(chave);
        return valor == null ? 1L : valor;
    }

    @Override
    public void expirar(String chave, Duration ttl) {
        redis.expire(chave, ttl);
    }

    @Override
    public String ler(String chave) {
        return redis.opsForValue().get(chave);
    }

    @Override
    public void gravar(String chave, String valor, Duration ttl) {
        redis.opsForValue().set(chave, valor, ttl);
    }

    @Override
    public boolean gravarSeAusente(String chave, String valor, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(chave, valor, ttl));
    }

    @Override
    public void remover(String chave) {
        redis.delete(chave);
    }
}
