package br.gov.ponto.common.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis em memória para testes (INCR/EXPIRE/GET/SET/DEL). Permite exercitar a lógica das
 * implementações distribuídas — inclusive o comportamento entre DUAS instâncias apontando para o
 * mesmo "servidor" — sem depender de um Redis no ar.
 */
public class OperacoesRedisFalso implements OperacoesRedis {

    public final Map<String, String> dados = new HashMap<>();
    public final Map<String, Duration> ttls = new HashMap<>();

    @Override
    public long incrementar(String chave) {
        long novo = Long.parseLong(dados.getOrDefault(chave, "0")) + 1;
        dados.put(chave, Long.toString(novo));
        return novo;
    }

    @Override
    public void expirar(String chave, Duration ttl) {
        ttls.put(chave, ttl);
    }

    @Override
    public String ler(String chave) {
        return dados.get(chave);
    }

    @Override
    public void gravar(String chave, String valor, Duration ttl) {
        dados.put(chave, valor);
        ttls.put(chave, ttl);
    }

    @Override
    public boolean gravarSeAusente(String chave, String valor, Duration ttl) {
        if (dados.containsKey(chave)) {
            return false;
        }
        gravar(chave, valor, ttl);
        return true;
    }

    @Override
    public void remover(String chave) {
        dados.remove(chave);
        ttls.remove(chave);
    }
}
