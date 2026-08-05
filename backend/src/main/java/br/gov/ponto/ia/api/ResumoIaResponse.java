package br.gov.ponto.ia.api;

/** Resumo executivo por IA. {@code disponivel=false} = recurso desligado ou sem provedor. */
public record ResumoIaResponse(boolean disponivel, String resumo) {

    public static ResumoIaResponse indisponivel() {
        return new ResumoIaResponse(false, "Resumo por IA não está habilitado neste ente.");
    }
}
