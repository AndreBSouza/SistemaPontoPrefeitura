package br.gov.ponto.ia;

/** Provedor de IA não configurado. Não deveria vazar para o usuário (serviços checam antes). */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException() {
        super("Provedor de IA não configurado neste ambiente.");
    }
}
