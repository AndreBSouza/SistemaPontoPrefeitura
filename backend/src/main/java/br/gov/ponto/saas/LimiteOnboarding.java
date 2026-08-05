package br.gov.ponto.saas;

import br.gov.ponto.common.util.ContadorJanela;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Limite anti-abuso para a solicitação pública de adesão de ente (self-service onboarding):
 * no máximo 5 solicitações por IP a cada hora. Endpoint pré-autenticado.
 *
 * <p>A contagem usa o {@link ContadorJanela}: com Redis configurado o limite é do sistema todo;
 * sem ele, é por instância.</p>
 */
@Component
public class LimiteOnboarding {

    private static final int MAX_POR_JANELA = 5;
    private static final Duration JANELA = Duration.ofHours(1);

    private final ContadorJanela contador;

    public LimiteOnboarding(ContadorJanela contador) {
        this.contador = contador;
    }

    public boolean permitir(String ip) {
        String chave = "limite:onboarding:" + (ip == null || ip.isBlank() ? "desconhecido" : ip);
        return contador.registrar(chave, JANELA) <= MAX_POR_JANELA;
    }
}
