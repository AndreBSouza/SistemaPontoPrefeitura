package br.gov.ponto.ativacao;

import br.gov.ponto.common.redis.OperacoesRedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Cache do token de dispositivo COMPARTILHADO entre instâncias (Redis).
 *
 * <p>Resolve o ponto crítico da operação multi-nó: {@link #invalidar(String)} apaga a chave no
 * Redis, então um aparelho revogado deixa de autenticar em TODAS as réplicas imediatamente (com o
 * cache em memória, cada nó só descobria ao expirar o TTL).</p>
 *
 * <p>Formato do valor: {@code tenantId|vinculoId|dispositivoId}, ou {@link #DESCONHECIDO} para
 * memoizar token inválido (evita marteladas no banco com token inexistente). Serialização simples
 * e explícita — nada de serialização binária de entidade JPA.</p>
 *
 * <p>Se o Redis estiver indisponível, cai no banco (o acesso continua correto, só mais lento) —
 * indisponibilidade de cache nunca pode virar falha de autenticação.</p>
 */
@Component
@Primary
@ConditionalOnBean(OperacoesRedis.class)
public class CacheDispositivoRedis implements CacheDispositivo {

    private static final Logger log = LoggerFactory.getLogger(CacheDispositivoRedis.class);
    private static final Duration TTL = Duration.ofSeconds(30);
    private static final String PREFIXO = "disp:";
    private static final String DESCONHECIDO = "-";

    private final OperacoesRedis redis;

    public CacheDispositivoRedis(OperacoesRedis redis) {
        this.redis = redis;
    }

    @Override
    public Optional<DispositivoPrincipal> obter(String chave,
                                                Supplier<Optional<DispositivoPrincipal>> carregar) {
        String k = PREFIXO + chave;
        try {
            String bruto = redis.ler(k);
            if (bruto != null) {
                return DESCONHECIDO.equals(bruto) ? Optional.empty() : desserializar(bruto);
            }
        } catch (RuntimeException e) {
            log.warn("Redis indisponível na leitura do cache de dispositivo ({}); consultando o banco.",
                    e.getClass().getSimpleName());
            return carregar.get();
        }

        Optional<DispositivoPrincipal> valor = carregar.get();
        try {
            redis.gravar(k, valor.map(CacheDispositivoRedis::serializar).orElse(DESCONHECIDO), TTL);
        } catch (RuntimeException e) {
            log.warn("Redis indisponível na gravação do cache de dispositivo ({}).",
                    e.getClass().getSimpleName());
        }
        return valor;
    }

    @Override
    public void invalidar(String chave) {
        // Falhar aqui em silêncio manteria um dispositivo revogado autenticando até o TTL:
        // propaga o erro para que a revogação não seja dada como concluída.
        redis.remover(PREFIXO + chave);
    }

    static String serializar(DispositivoPrincipal p) {
        return p.tenantId() + "|" + p.vinculoId() + "|" + p.dispositivoId();
    }

    static Optional<DispositivoPrincipal> desserializar(String bruto) {
        String[] partes = bruto.split("\\|");
        if (partes.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new DispositivoPrincipal(
                    UUID.fromString(partes[0]), UUID.fromString(partes[1]), UUID.fromString(partes[2])));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // valor corrompido: trata como cache miss seguro
        }
    }
}
