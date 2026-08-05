package br.gov.ponto.common.agendador;

import br.gov.ponto.common.redis.OperacoesRedis;
import br.gov.ponto.common.redis.OperacoesRedisFalso;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TravaDistribuidaTest {

    private static final Duration MEIA_HORA = Duration.ofMinutes(30);

    @Test
    void travaLocalSempreAssume() {
        assertThat(new TravaLocal().assumir("tarefa", MEIA_HORA)).isTrue();
    }

    @Test
    void apenasUmaInstanciaAssumeComRedis() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso(); // mesmo Redis para as 3 réplicas
        TravaRedis no1 = new TravaRedis(redis);
        TravaRedis no2 = new TravaRedis(redis);
        TravaRedis no3 = new TravaRedis(redis);

        assertThat(no1.assumir("lembretes", MEIA_HORA)).isTrue();
        // As outras desistem — senão cada servidor receberia 3 notificações iguais.
        assertThat(no2.assumir("lembretes", MEIA_HORA)).isFalse();
        assertThat(no3.assumir("lembretes", MEIA_HORA)).isFalse();
    }

    @Test
    void tarefasDiferentesNaoSeBloqueiam() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso();
        TravaRedis trava = new TravaRedis(redis);

        assertThat(trava.assumir("lembretes", MEIA_HORA)).isTrue();
        assertThat(trava.assumir("expurgo", MEIA_HORA)).isTrue();
    }

    @Test
    void proximaJanelaPodeAssumirDeNovo() {
        OperacoesRedisFalso redis = new OperacoesRedisFalso();
        TravaRedis trava = new TravaRedis(redis);

        assertThat(trava.assumir("lembretes", MEIA_HORA)).isTrue();
        redis.remover("trava:lembretes"); // TTL venceu
        assertThat(trava.assumir("lembretes", MEIA_HORA)).isTrue();
    }

    @Test
    void redisForaDoArNaoAssumeParaNaoDuplicarNotificacao() {
        OperacoesRedis quebrado = new OperacoesRedis() {
            @Override public long incrementar(String c) { throw new IllegalStateException("fora do ar"); }
            @Override public void expirar(String c, Duration t) { throw new IllegalStateException("fora do ar"); }
            @Override public String ler(String c) { throw new IllegalStateException("fora do ar"); }
            @Override public void gravar(String c, String v, Duration t) { throw new IllegalStateException("fora do ar"); }
            @Override public boolean gravarSeAusente(String c, String v, Duration t) { throw new IllegalStateException("fora do ar"); }
            @Override public void remover(String c) { throw new IllegalStateException("fora do ar"); }
        };
        assertThat(new TravaRedis(quebrado).assumir("lembretes", MEIA_HORA)).isFalse();
    }
}
