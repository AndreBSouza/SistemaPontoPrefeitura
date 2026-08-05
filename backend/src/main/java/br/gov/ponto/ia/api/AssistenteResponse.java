package br.gov.ponto.ia.api;

/** Resposta do assistente. {@code disponivel=false} = recurso desligado ou sem provedor. */
public record AssistenteResponse(boolean disponivel, String resposta) {

    public static AssistenteResponse indisponivel() {
        return new AssistenteResponse(false, "Assistente de IA não está habilitado neste ente.");
    }
}
