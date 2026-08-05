package br.gov.ponto.common.util;

import java.time.Duration;

/**
 * Contador de janela fixa usado pelos limites anti-abuso pré-autenticados (ativação, adesão).
 *
 * <p>Com mais de uma instância da aplicação, um contador em memória divide o limite por réplica
 * (3 nós = 3× mais tentativas permitidas). A implementação Redis compartilha a contagem entre
 * todas as instâncias, então o limite vale para o sistema inteiro.</p>
 */
public interface ContadorJanela {

    /**
     * Registra uma ocorrência para a chave dentro da janela e devolve o total acumulado.
     *
     * @return quantas ocorrências já houve na janela corrente (incluindo esta)
     */
    long registrar(String chave, Duration janela);
}
