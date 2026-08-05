package br.gov.ponto.relatorios;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Assinatura digital <b>CAdES/PKCS#7 (CMS)</b> do AFD com certificado <b>ICP-Brasil</b>
 * (e-CNPJ do ente — keystore PKCS#12 A1). Produz uma assinatura DESTACADA (detached) em DER,
 * devolvida em base64 — o arquivo .p7s do AFD.
 *
 * <p>Bean {@code @Primary} que só é registrado quando {@code assinatura.keystore} está definido
 * (perfil de produção com certificado). Sem isso, prevalece o {@link AssinaturaIndisponivel}
 * (no-op) e o gating de assinatura fica intacto — nada mais no código muda. Ver
 * {@code docs/afd-assinatura-icp-brasil.md}.</p>
 *
 * <p>Config: {@code assinatura.keystore} (caminho do .p12), {@code assinatura.senha},
 * {@code assinatura.alias} (opcional; usa o primeiro alias com chave privada se ausente).
 * O certificado deve ser um e-CNPJ ICP-Brasil de AC credenciada — a validade jurídica vem do
 * certificado, não do código.</p>
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "assinatura", name = "keystore")
public class AssinaturaCadesService implements AssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaCadesService.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final PrivateKey chavePrivada;
    private final X509Certificate certificado;
    private final Store<?> cadeiaCertificados;

    public AssinaturaCadesService(
            @Value("${assinatura.keystore}") String keystore,
            @Value("${assinatura.senha:}") String senha,
            @Value("${assinatura.alias:}") String alias) {
        try {
            char[] pin = senha == null ? new char[0] : senha.toCharArray();
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(Path.of(keystore))) {
                ks.load(in, pin);
            }
            String al = (alias == null || alias.isBlank()) ? primeiroAliasComChave(ks) : alias;
            this.chavePrivada = (PrivateKey) ks.getKey(al, pin);
            if (this.chavePrivada == null) {
                throw new IllegalStateException("Alias '" + al + "' não possui chave privada no keystore");
            }
            Certificate[] chain = ks.getCertificateChain(al);
            if (chain == null || chain.length == 0) {
                throw new IllegalStateException("Keystore sem cadeia de certificados para o alias '" + al + "'");
            }
            this.certificado = (X509Certificate) chain[0];
            List<Certificate> lista = new ArrayList<>(List.of(chain));
            this.cadeiaCertificados = new JcaCertStore(lista);
            log.info("Assinatura CAdES habilitada (certificado: {}).", certificado.getSubjectX500Principal().getName());
        } catch (Exception e) {
            // Falha na configuração do certificado deve derrubar o startup (fail-fast), não gerar AFD sem assinar.
            throw new IllegalStateException("Não foi possível inicializar a assinatura CAdES (assinatura.keystore): "
                    + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public Optional<String> assinar(byte[] conteudo) {
        try {
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build("SHA256withRSA", chavePrivada, certificado));
            gen.addCertificates(cadeiaCertificados);
            // false = assinatura destacada (não encapsula o conteúdo do AFD no .p7s).
            CMSSignedData assinado = gen.generate(new CMSProcessableByteArray(conteudo), false);
            return Optional.of(Base64.getEncoder().encodeToString(assinado.getEncoded()));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar em CAdES: " + e.getClass().getSimpleName(), e);
        }
    }

    private static String primeiroAliasComChave(KeyStore ks) throws Exception {
        var aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String a = aliases.nextElement();
            if (ks.isKeyEntry(a)) {
                return a;
            }
        }
        throw new IllegalStateException("Keystore não contém nenhuma entrada com chave privada");
    }
}
