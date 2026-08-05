package br.gov.ponto.common.agendador;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Trava local (padrão, instância única): sempre assume, porque não há concorrente.
 *
 * <p>Com mais de uma réplica é obrigatório o Redis ({@link TravaRedis}) — senão cada instância
 * executa a mesma rotina e o servidor recebe notificações duplicadas.</p>
 */
@Component
public class TravaLocal implements TravaDistribuida {

    @Override
    public boolean assumir(String tarefa, Duration duracao) {
        return true;
    }
}
