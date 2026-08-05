package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.rep.CertificadoIcpBrasil;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assinatura embutida no PDF (PAdES), exigida pelo art. 80, I, para o comprovante eletrônico.
 */
class AssinadorPdfTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static CertificadoIcpBrasil certificadoDeTeste() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X500Name dn = new X500Name("CN=Fornecedor Teste, O=Teste, C=BR");
        Date de = new Date();
        Date ate = new Date(de.getTime() + 365L * 24 * 3600 * 1000);
        ContentSigner cs = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(kp.getPrivate());
        X509CertificateHolder holder = new JcaX509v3CertificateBuilder(
                dn, BigInteger.valueOf(System.nanoTime()), de, ate, dn, kp.getPublic()).build(cs);
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);

        return new CertificadoIcpBrasil(kp.getPrivate(), new Certificate[]{cert});
    }

    /** PDF mínimo válido para servir de entrada da assinatura. */
    private static byte[] pdfSimples() throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, saida);
        doc.open();
        doc.add(new Paragraph("Comprovante de Registro de Ponto do Trabalhador"));
        doc.close();
        return saida.toByteArray();
    }

    @Test
    void semCertificadoDevolveOPdfIntactoEAvisaQueNaoAssinou() throws Exception {
        byte[] original = pdfSimples();
        AssinadorPdf assinador = new AssinadorPdfIndisponivel();

        assertThat(assinador.disponivel()).isFalse();
        assertThat(assinador.assinar(original)).isEqualTo(original);
    }

    @Test
    void comCertificadoEmbuteAAssinaturaNoProprioPdf() throws Exception {
        byte[] assinado = new AssinadorPdfIcpBrasil(certificadoDeTeste()).assinar(pdfSimples());

        // Continua sendo um PDF legível...
        assertThat(new String(assinado, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");

        // ...e agora com um campo de assinatura que qualquer leitor reconhece.
        PdfReader leitor = new PdfReader(assinado);
        AcroFields campos = leitor.getAcroFields();
        assertThat(campos.getSignatureNames())
                .as("o comprovante eletronico precisa sair assinado (art. 80, I)")
                .isNotEmpty();

        String nomeDaAssinatura = campos.getSignatureNames().get(0);
        assertThat(campos.verifySignature(nomeDaAssinatura).getSigningCertificate()
                .getSubjectX500Principal().getName()).contains("Fornecedor Teste");
        leitor.close();
    }

    @Test
    void assinaturaCobreOConteudoDoDocumento() throws Exception {
        byte[] assinado = new AssinadorPdfIcpBrasil(certificadoDeTeste()).assinar(pdfSimples());

        PdfReader leitor = new PdfReader(assinado);
        AcroFields campos = leitor.getAcroFields();
        String nome = campos.getSignatureNames().get(0);
        // A assinatura precisa abranger o documento inteiro; se cobrisse só parte dele, daria para
        // anexar conteudo depois sem invalidar a assinatura.
        assertThat(campos.signatureCoversWholeDocument(nome)).isTrue();
        leitor.close();
    }
}
