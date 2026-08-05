package br.gov.ponto.ia;

import java.util.UUID;

/**
 * Porta (Inversão de Dependência): verifica o consentimento LGPD do servidor para uma finalidade.
 * A camada de IA declara a necessidade; a camada {@code me} a implementa ({@code MeService}) — assim
 * {@code ia} não depende de {@code me} (evita ciclo de pacote).
 */
public interface ConsentimentoServidor {

    boolean consentimento(UUID vinculoId, String finalidade);
}
