package br.gov.ponto.ia;

/** Falha em runtime ao consultar o provedor de IA (rede, 4xx/5xx, resposta inesperada). */
public class IaFalhaException extends RuntimeException {

    public IaFalhaException(String mensagem) {
        super(mensagem);
    }

    public IaFalhaException(Throwable causa) {
        super("Falha ao consultar o provedor de IA.", causa);
    }
}
