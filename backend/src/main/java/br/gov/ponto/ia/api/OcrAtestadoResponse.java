package br.gov.ponto.ia.api;

import br.gov.ponto.ia.OcrAtestado;

/** Resultado do OCR de atestado. {@code disponivel=false} = recurso desligado ou sem provedor. */
public record OcrAtestadoResponse(boolean disponivel, OcrAtestado dados) {

    public static OcrAtestadoResponse indisponivel() {
        return new OcrAtestadoResponse(false, null);
    }
}
