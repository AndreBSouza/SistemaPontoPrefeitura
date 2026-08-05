package br.gov.ponto.ativacao;

import br.gov.ponto.common.util.ContadorJanela;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Limite anti-brute-force para a ativação (POST /api/ativacao/ativar), que é
 * pré-autenticada: no máximo 10 tentativas por IP a cada 60s.
 *
 * <p>A contagem usa o {@link ContadorJanela}: com Redis configurado o limite é do sistema todo;
 * sem ele, é por instância.</p>
 */
@Component
public class LimiteAtivacao {

    private static final int MAX_POR_JANELA = 10;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private final ContadorJanela contador;

    public LimiteAtivacao(ContadorJanela contador) {
        this.contador = contador;
    }

    public boolean permitir(String ip) {
        String chave = "limite:ativacao:" + (ip == null || ip.isBlank() ? "desconhecido" : ip);
        return contador.registrar(chave, JANELA) <= MAX_POR_JANELA;
    }
}
