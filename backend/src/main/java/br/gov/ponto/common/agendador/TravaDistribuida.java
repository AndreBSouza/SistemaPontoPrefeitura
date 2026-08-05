package br.gov.ponto.common.agendador;

import java.time.Duration;

/**
 * Garante que uma rotina agendada rode em UMA instância por vez.
 *
 * <p>Sem isso, com N réplicas a mesma rotina dispara N vezes — no caso dos lembretes, o servidor
 * receberia N notificações iguais.</p>
 */
public interface TravaDistribuida {

    /**
     * Tenta assumir a tarefa. Devolve {@code true} para exatamente uma instância dentro do período
     * de {@code duracao}.
     */
    boolean assumir(String tarefa, Duration duracao);
}
