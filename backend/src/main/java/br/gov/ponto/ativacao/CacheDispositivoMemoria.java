package br.gov.ponto.ativacao;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Cache em memória (padrão, instância única). A revogação só invalida a entrada DESTE nó — com
 * mais de uma réplica, configure o Redis para que a revogação seja imediata em todas.
 */
@Component
public class CacheDispositivoMemoria implements CacheDispositivo {

    static final long TTL_MS = 30_000;

    private record Entrada(Optional<DispositivoPrincipal> valor, long expiraEm) {
    }

    private final Map<String, Entrada> mapa = new ConcurrentHashMap<>();
    private final LongSupplier relogio;

    public CacheDispositivoMemoria() {
        this(System::currentTimeMillis);
    }

    /** Construtor de teste: permite controlar a expiração. */
    CacheDispositivoMemoria(LongSupplier relogio) {
        this.relogio = relogio;
    }

    @Override
    public Optional<DispositivoPrincipal> obter(String chave,
                                                Supplier<Optional<DispositivoPrincipal>> carregar) {
        long agora = relogio.getAsLong();
        Entrada e = mapa.get(chave);
        if (e != null && e.expiraEm() > agora) {
            return e.valor();
        }
        Optional<DispositivoPrincipal> valor = carregar.get();
        mapa.put(chave, new Entrada(valor, agora + TTL_MS));
        return valor;
    }

    @Override
    public void invalidar(String chave) {
        mapa.remove(chave);
    }
}
