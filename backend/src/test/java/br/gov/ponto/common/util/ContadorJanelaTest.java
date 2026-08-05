package br.gov.ponto.common.util;

import br.gov.ponto.common.redis.OperacoesRedisFalso;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre as duas implementações do contador de janela fixa com a MESMA semântica: conta dentro da
 * janela, reabre na janela seguinte e mantém chaves independentes.
 */
class ContadorJanelaTest {

    private static final Duration JANELA = Duration.ofSeconds(1);

    // ---------- em memória ----------

    @Test
    void memoriaContaDentroDaJanelaEReabreNaSeguinte() {
        AtomicLong agora = new AtomicLong(0);
        ContadorJanelaMemoria c = new ContadorJanelaMemoria(agora::get);

        assertThat(c.registrar("ip", JANELA)).isEqualTo(1);
        agora.set(100);
        assertThat(c.registrar("ip", JANELA)).isEqualTo(2);
        agora.set(200);
        assertThat(c.registrar("ip", JANELA)).isEqualTo(3);

        agora.set(1001); // nova janela
        assertThat(c.registrar("ip", JANELA)).isEqualTo(1);
    }

    @Test
    void memoriaMantemChavesIndependentes() {
        ContadorJanelaMemoria c = new ContadorJanelaMemoria(() -> 0L);
        assertThat(c.registrar("a", JANELA)).isEqualTo(1);
        assertThat(c.registrar("b", JANELA)).isEqualTo(1);
        assertThat(c.registrar("a", JANELA)).isEqualTo(2);
    }

    // ---------- Redis ----------

    @Test
    void redisAcumulaEDefineOTtlUmaVezSo() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso();
        ContadorJanelaRedis c = new ContadorJanelaRedis(redis);

        assertThat(c.registrar("ip", JANELA)).isEqualTo(1);
        assertThat(redis.ttls).containsEntry("ip", JANELA); // TTL definido na 1ª ocorrência

        redis.ttls.clear(); // se o TTL fosse redefinido a cada hit, a janela nunca fecharia
        assertThat(c.registrar("ip", JANELA)).isEqualTo(2);
        assertThat(c.registrar("ip", JANELA)).isEqualTo(3);
        assertThat(redis.ttls).isEmpty();
    }

    @Test
    void redisCompartilhaAContagemEntreInstancias() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso(); // o mesmo "servidor" para as duas instâncias
        ContadorJanelaRedis no1 = new ContadorJanelaRedis(redis);
        ContadorJanelaRedis no2 = new ContadorJanelaRedis(redis);

        no1.registrar("ip", JANELA);
        no1.registrar("ip", JANELA);
        // A 3ª tentativa cai em outra réplica e mesmo assim conta no total — este é o ponto:
        // com contador em memória cada nó recomeçaria do 1 e o limite valeria 3× mais.
        assertThat(no2.registrar("ip", JANELA)).isEqualTo(3);
    }

    @Test
    void janelaExpiradaNoRedisRecomecaAContagem() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso();
        ContadorJanelaRedis c = new ContadorJanelaRedis(redis);
        c.registrar("ip", JANELA);
        c.registrar("ip", JANELA);

        redis.remover("ip"); // TTL venceu

        assertThat(c.registrar("ip", JANELA)).isEqualTo(1);
        assertThat(redis.ttls).containsEntry("ip", JANELA);
    }
}
