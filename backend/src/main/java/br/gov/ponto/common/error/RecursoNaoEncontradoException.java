package br.gov.ponto.common.error;

/** Recurso inexistente (mapeada para HTTP 404). */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
