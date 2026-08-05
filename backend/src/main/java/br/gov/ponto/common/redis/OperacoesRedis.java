package br.gov.ponto.common.redis;

import java.time.Duration;

/**
 * Porta fina sobre o Redis (só as operações que o sistema usa).
 *
 * <p>Existe para que a lógica de rate-limit e de cache distribuído seja testável sem um servidor
 * Redis no ar — os testes injetam uma implementação em memória; a produção usa
 * {@link OperacoesRedisSpring}.</p>
 */
public interface OperacoesRedis {

    /** Incrementa o contador da chave (criando com 1 se não existir) e devolve o valor novo. */
    long incrementar(String chave);

    /** Define o tempo de vida da chave. */
    void expirar(String chave, Duration ttl);

    /** Valor da chave, ou {@code null} se ausente/expirada. */
    String ler(String chave);

    /** Grava o valor com tempo de vida. */
    void gravar(String chave, String valor, Duration ttl);

    /**
     * Grava SOMENTE se a chave não existir (SET NX EX) e diz se conseguiu.
     * É a primitiva de trava distribuída: só uma instância vence a corrida.
     */
    boolean gravarSeAusente(String chave, String valor, Duration ttl);

    /** Remove a chave (propaga a invalidação para todas as instâncias). */
    void remover(String chave);
}
