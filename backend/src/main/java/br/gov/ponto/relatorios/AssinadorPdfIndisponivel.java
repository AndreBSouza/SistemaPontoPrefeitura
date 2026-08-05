package br.gov.ponto.relatorios;

import org.springframework.stereotype.Service;

/**
 * Default (dev / sem certificado): devolve o PDF sem assinar.
 *
 * <p>O comprovante ainda é gerado e entregue — o que muda é que ele sai marcado como NÃO assinado,
 * em vez de fingir conformidade com o art. 80, I.</p>
 */
@Service
public class AssinadorPdfIndisponivel implements AssinadorPdf {

    @Override
    public byte[] assinar(byte[] pdf) {
        return pdf;
    }

    @Override
    public boolean disponivel() {
        return false;
    }
}
