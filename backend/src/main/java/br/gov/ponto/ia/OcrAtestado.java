package br.gov.ponto.ia;

import java.time.LocalDate;

/** Dados extraídos de um atestado médico por OCR/IA (para pré-preencher a justificativa). */
public record OcrAtestado(String cid, LocalDate inicio, int dias, String profissional,
                          String registro, String textoBruto) {
}
