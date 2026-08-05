package br.gov.ponto.common.error;

/** Violacao de regra de unicidade/negocio (mapeada para HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String message) {
        super(message);
    }
}
