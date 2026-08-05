package br.gov.ponto.relatorios;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.util.Store;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prova que a assinatura CAdES é uma assinatura CMS válida e verificável sobre o conteúdo do AFD.
 * Gera um certificado self-signed + keystore PKCS#12 em arquivo temporário (sem depender de e-CNPJ).
 */
class AssinaturaCadesServiceTest {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private Path criarKeystore(String senha, String alias) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X500Name dn = new X500Name("CN=Prefeitura Teste, O=Ente Teste, C=BR");
        Date de = new Date();
        Date ate = new Date(de.getTime() + 365L * 24 * 3600 * 1000);
        ContentSigner cs = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(kp.getPrivate());
        X509CertificateHolder holder = new JcaX509v3CertificateBuilder(
                dn, BigInteger.valueOf(System.nanoTime()), de, ate, dn, kp.getPublic()).build(cs);
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(holder);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(alias, kp.getPrivate(), senha.toCharArray(), new Certificate[]{cert});
        Path arq = Files.createTempFile("ks-teste", ".p12");
        arq.toFile().deleteOnExit();
        try (OutputStream os = Files.newOutputStream(arq)) {
            ks.store(os, senha.toCharArray());
        }
        return arq;
    }

    @Test
    void assinaEProduzUmCmsDestacadoVerificavel() throws Exception {
        Path ks = criarKeystore("senha123", "ente");
        AssinaturaCadesService servico = new AssinaturaCadesService(ks.toString(), "senha123", "ente");

        byte[] afd = "000000000110000... conteudo do AFD ...".getBytes(StandardCharsets.UTF_8);
        Optional<String> assinatura = servico.assinar(afd);

        assertThat(assinatura).isPresent();

        // Reconstrói o CMS destacado com o conteúdo original e verifica a assinatura.
        byte[] der = Base64.getDecoder().decode(assinatura.get());
        CMSSignedData sd = new CMSSignedData(new CMSProcessableByteArray(afd), der);
        Store<X509CertificateHolder> certs = sd.getCertificates();
        SignerInformation signer = sd.getSignerInfos().getSigners().iterator().next();
        X509CertificateHolder cert = (X509CertificateHolder) certs.getMatches(signer.getSID()).iterator().next();

        boolean valido = signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).build(cert));
        assertThat(valido).isTrue();
    }

    @Test
    void usaOPrimeiroAliasComChaveQuandoAliasNaoInformado() throws Exception {
        Path ks = criarKeystore("s3nha", "qualquer");
        AssinaturaCadesService servico = new AssinaturaCadesService(ks.toString(), "s3nha", "");
        assertThat(servico.assinar("x".getBytes(StandardCharsets.UTF_8))).isPresent();
    }
}
