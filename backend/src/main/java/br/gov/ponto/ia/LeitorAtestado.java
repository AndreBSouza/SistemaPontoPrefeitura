package br.gov.ponto.ia;

import br.gov.ponto.ia.api.OcrAtestadoResponse;

/** Porta do OCR de atestado (implementada por {@link OcrAtestadoService}); permite testar o gate isolado. */
public interface LeitorAtestado {

    OcrAtestadoResponse ler(byte[] imagem, String contentType);
}
