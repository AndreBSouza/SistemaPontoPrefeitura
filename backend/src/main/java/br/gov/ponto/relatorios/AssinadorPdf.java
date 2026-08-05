package br.gov.ponto.relatorios;

/**
 * Assinatura eletrônica de um documento PDF.
 *
 * <p>O art. 80, I, exige que o Comprovante de Registro de Ponto do Trabalhador em formato
 * eletrônico seja PDF <b>assinado</b>, e o art. 88 exige certificado ICP-Brasil. A assinatura é
 * embutida no próprio arquivo (PAdES), para que qualquer leitor de PDF a exiba.</p>
 */
public interface AssinadorPdf {

    /**
     * Devolve o PDF assinado. Sem certificado configurado, devolve o PDF original — o documento
     * continua válido como informação, mas sem valor probatório de assinatura.
     */
    byte[] assinar(byte[] pdf);

    /** Indica se o PDF sai efetivamente assinado (usado para avisar no documento). */
    boolean disponivel();
}
