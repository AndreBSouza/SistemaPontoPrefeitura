package br.gov.ponto.common.error;

/**
 * O titular precisa consentir com uma finalidade antes da ação (mapeada para HTTP 403 com o
 * código {@code CONSENTIMENTO_NECESSARIO} + a finalidade, para o app pedir o consentimento e repetir).
 */
public class ConsentimentoNecessarioException extends RuntimeException {

    private final String finalidade;

    public ConsentimentoNecessarioException(String finalidade, String message) {
        super(message);
        this.finalidade = finalidade;
    }

    public String getFinalidade() {
        return finalidade;
    }
}
