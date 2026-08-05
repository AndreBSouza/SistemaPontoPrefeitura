package br.gov.ponto.common.util;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Contador em memória (padrão, instância única). Cada réplica conta separadamente — para
 * escalar horizontalmente, configure o Redis (ver {@link ContadorJanelaRedis}).
 */
@Component
public class ContadorJanelaMemoria implements ContadorJanela {

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final LongSupplier relogio;

    public ContadorJanelaMemoria() {
        this(System::currentTimeMillis);
    }

    /** Construtor de teste: permite controlar a passagem do tempo. */
    ContadorJanelaMemoria(LongSupplier relogio) {
        this.relogio = relogio;
    }

    @Override
    public long registrar(String chave, Duration janela) {
        long agora = relogio.getAsLong();
        long duracao = janela.toMillis();
        Janela atual = janelas.compute(chave, (k, j) -> {
            if (j == null || agora - j.inicio >= duracao) {
                return new Janela(agora, 1);
            }
            j.contagem++;
            return j;
        });
        return atual.contagem;
    }

    private static final class Janela {
        final long inicio;
        int contagem;

        Janela(long inicio, int contagem) {
            this.inicio = inicio;
            this.contagem = contagem;
        }
    }
}
