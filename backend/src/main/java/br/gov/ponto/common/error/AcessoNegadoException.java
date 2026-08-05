package br.gov.ponto.common.error;

/** Ação não permitida para o solicitante (mapeada para HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String message) {
        super(message);
    }
}
