package br.gov.ponto.ativacao;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache (TTL curto) da validação do token de dispositivo, para evitar uma consulta ao banco a
 * cada requisição autenticada do app.
 *
 * <p>O valor guardado é o {@link DispositivoPrincipal} (record imutável com os 3 identificadores
 * usados na autenticação), não a entidade JPA — assim a implementação distribuída pode serializá-lo
 * com segurança.</p>
 *
 * <p><b>Revogação:</b> {@link #invalidar(String)} precisa valer para TODAS as instâncias, senão um
 * aparelho revogado continua entrando por até o TTL nos outros nós. A implementação em memória só
 * serve para instância única; com Redis configurado a invalidação é global.</p>
 */
public interface CacheDispositivo {

    /** Retorna do cache (se válido) ou carrega via {@code carregar} e memoiza. */
    Optional<DispositivoPrincipal> obter(String chave, Supplier<Optional<DispositivoPrincipal>> carregar);

    /** Invalida a entrada (revogação do dispositivo). */
    void invalidar(String chave);
}
