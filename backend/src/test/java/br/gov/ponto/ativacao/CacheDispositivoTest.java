package br.gov.ponto.ativacao;

import br.gov.ponto.common.redis.OperacoesRedisFalso;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre as duas implementações do cache de dispositivo com a mesma semântica (memoiza, expira,
 * invalida) e a diferença que importa em produção: com Redis, a revogação vale para TODAS as
 * instâncias imediatamente.
 */
class CacheDispositivoTest {

    private static DispositivoPrincipal principal() {
        return new DispositivoPrincipal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    // ---------- em memória ----------

    @Test
    void memoriaSegundaConsultaVemDoCache() {
        CacheDispositivoMemoria cache = new CacheDispositivoMemoria();
        AtomicInteger chamadas = new AtomicInteger();
        Supplier<Optional<DispositivoPrincipal>> carregar = () -> {
            chamadas.incrementAndGet();
            return Optional.of(principal());
        };
        cache.obter("chave", carregar);
        cache.obter("chave", carregar);
        assertThat(chamadas.get()).isEqualTo(1);
    }

    @Test
    void memoriaInvalidarForcaNovaConsulta() {
        CacheDispositivoMemoria cache = new CacheDispositivoMemoria();
        AtomicInteger chamadas = new AtomicInteger();
        Supplier<Optional<DispositivoPrincipal>> carregar = () -> {
            chamadas.incrementAndGet();
            return Optional.empty();
        };
        cache.obter("chave", carregar);
        cache.invalidar("chave");
        cache.obter("chave", carregar);
        assertThat(chamadas.get()).isEqualTo(2);
    }

    @Test
    void memoriaRecarregaDepoisDoTtl() {
        AtomicLong agora = new AtomicLong(0);
        CacheDispositivoMemoria cache = new CacheDispositivoMemoria(agora::get);
        AtomicInteger chamadas = new AtomicInteger();
        Supplier<Optional<DispositivoPrincipal>> carregar = () -> {
            chamadas.incrementAndGet();
            return Optional.of(principal());
        };
        cache.obter("chave", carregar);
        agora.set(CacheDispositivoMemoria.TTL_MS + 1);
        cache.obter("chave", carregar);
        assertThat(chamadas.get()).isEqualTo(2);
    }

    // ---------- Redis ----------

    @Test
    void redisMemoizaEPreservaOsIdentificadores() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso();
        CacheDispositivoRedis cache = new CacheDispositivoRedis(redis);
        DispositivoPrincipal p = principal();
        AtomicInteger chamadas = new AtomicInteger();
        Supplier<Optional<DispositivoPrincipal>> carregar = () -> {
            chamadas.incrementAndGet();
            return Optional.of(p);
        };

        assertThat(cache.obter("hash", carregar)).contains(p);
        assertThat(cache.obter("hash", carregar)).contains(p); // veio do Redis, com os 3 UUIDs
        assertThat(chamadas.get()).isEqualTo(1);
    }

    @Test
    void redisMemoizaTokenDesconhecidoSemMartelarOBanco() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso();
        CacheDispositivoRedis cache = new CacheDispositivoRedis(redis);
        AtomicInteger chamadas = new AtomicInteger();
        Supplier<Optional<DispositivoPrincipal>> carregar = () -> {
            chamadas.incrementAndGet();
            return Optional.empty();
        };

        assertThat(cache.obter("hash", carregar)).isEmpty();
        assertThat(cache.obter("hash", carregar)).isEmpty();
        assertThat(chamadas.get()).isEqualTo(1);
    }

    @Test
    void revogacaoNoRedisValeParaTodasAsInstancias() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso(); // mesmo servidor para os dois nós
        CacheDispositivoRedis no1 = new CacheDispositivoRedis(redis);
        CacheDispositivoRedis no2 = new CacheDispositivoRedis(redis);
        DispositivoPrincipal p = principal();

        no1.obter("hash", () -> Optional.of(p));
        assertThat(no2.obter("hash", () -> Optional.of(p))).contains(p);

        no1.invalidar("hash"); // revogação acontece no nó 1...

        // ...e o nó 2 já não serve do cache: recarrega e enxerga o dispositivo revogado.
        assertThat(no2.obter("hash", Optional::empty)).isEmpty();
    }

    @Test
    void redisIndisponivelCaiNoBancoEmVezDeNegarAcesso() {
        OperacoesRedisQuebrado redis = new OperacoesRedisQuebrado();
        CacheDispositivoRedis cache = new CacheDispositivoRedis(redis);
        DispositivoPrincipal p = principal();

        assertThat(cache.obter("hash", () -> Optional.of(p))).contains(p);
    }

    /** Redis fora do ar: toda operação de leitura/escrita falha. */
    static class OperacoesRedisQuebrado implements br.gov.ponto.common.redis.OperacoesRedis {
        @Override
        public long incrementar(String chave) {
            throw new IllegalStateException("redis fora do ar");
        }

        @Override
        public void expirar(String chave, java.time.Duration ttl) {
            throw new IllegalStateException("redis fora do ar");
        }

        @Override
        public String ler(String chave) {
            throw new IllegalStateException("redis fora do ar");
        }

        @Override
        public void gravar(String chave, String valor, java.time.Duration ttl) {
            throw new IllegalStateException("redis fora do ar");
        }

        @Override
        public boolean gravarSeAusente(String chave, String valor, java.time.Duration ttl) {
            throw new IllegalStateException("redis fora do ar");
        }

        @Override
        public void remover(String chave) {
            throw new IllegalStateException("redis fora do ar");
        }
    }
}
