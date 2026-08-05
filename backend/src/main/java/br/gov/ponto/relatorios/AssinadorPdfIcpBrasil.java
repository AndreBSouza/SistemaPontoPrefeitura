package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.rep.CertificadoIcpBrasil;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Assinatura embutida no PDF (PAdES) com certificado <b>ICP-Brasil</b>, exigida pelo art. 80, I,
 * combinado com o art. 88, para o Comprovante de Registro de Ponto do Trabalhador em formato
 * eletrônico.
 *
 * <p>A assinatura vai DENTRO do arquivo, então qualquer leitor de PDF a reconhece e mostra o
 * signatário — diferente do AFD, que usa assinatura destacada em {@code .p7s} porque é texto puro
 * de largura fixa e não comporta a assinatura no próprio corpo.</p>
 */
@Service
@Primary
@ConditionalOnBean(CertificadoIcpBrasil.class)
public class AssinadorPdfIcpBrasil implements AssinadorPdf {

    private static final Logger log = LoggerFactory.getLogger(AssinadorPdfIcpBrasil.class);

    private final CertificadoIcpBrasil certificado;

    public AssinadorPdfIcpBrasil(CertificadoIcpBrasil certificado) {
        this.certificado = certificado;
        log.info("Assinatura de PDF (PAdES) habilitada para o comprovante do trabalhador.");
    }

    @Override
    public byte[] assinar(byte[] pdf) {
        try {
            PdfReader leitor = new PdfReader(pdf);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            // '\0' = mantém a versão do PDF; true = assinatura em modo append, preservando o
            // conteúdo original byte a byte (requisito para a assinatura permanecer verificável).
            PdfStamper stamper = PdfStamper.createSignature(leitor, saida, '\0', null, true);

            PdfSignatureAppearance aparencia = stamper.getSignatureAppearance();
            aparencia.setCrypto(certificado.chavePrivada(), certificado.cadeia(), null,
                    PdfSignatureAppearance.WINCER_SIGNED);
            aparencia.setReason("Comprovante de Registro de Ponto do Trabalhador");
            aparencia.setLocation("Registrador Eletronico de Ponto via Programa (REP-P)");
            // Assinatura invisível: o comprovante é um documento pequeno e a validade é conferida
            // pelo painel de assinaturas do leitor, não por um carimbo na página.
            stamper.close();
            leitor.close();
            return saida.toByteArray();
        } catch (Exception e) {
            // Falhar em silêncio entregaria um comprovante sem valor probatório passando por válido.
            throw new IllegalStateException(
                    "Falha ao assinar o comprovante em PDF: " + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public boolean disponivel() {
        return true;
    }
}
