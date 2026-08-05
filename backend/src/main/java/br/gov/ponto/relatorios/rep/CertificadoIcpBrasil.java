package br.gov.ponto.relatorios.rep;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Certificado <b>ICP-Brasil</b> do e-CNPJ (keystore PKCS#12 A1) usado para assinar as saídas do
 * REP-P e do PTRP: o AFD/AEJ em CAdES destacado e o comprovante do trabalhador em PDF.
 *
 * <p>Carrega a keystore UMA vez e é compartilhado pelos assinadores — antes cada um abriria o
 * arquivo por conta própria, com dois pontos de configuração para o mesmo certificado.</p>
 *
 * <p>Só é registrado quando {@code assinatura.keystore} está definido. Uma keystore inválida
 * derruba o startup de propósito: melhor não subir do que emitir documento sem assinar achando
 * que assinou.</p>
 */
@Component
@ConditionalOnProperty(prefix = "assinatura", name = "keystore")
public class CertificadoIcpBrasil {

    private static final Logger log = LoggerFactory.getLogger(CertificadoIcpBrasil.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final PrivateKey chavePrivada;
    private final Certificate[] cadeia;

    public CertificadoIcpBrasil(@Value("${assinatura.keystore}") String keystore,
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
            if (chavePrivada == null) {
                throw new IllegalStateException("Alias '" + al + "' não possui chave privada no keystore");
            }
            this.cadeia = ks.getCertificateChain(al);
            if (cadeia == null || cadeia.length == 0) {
                throw new IllegalStateException("Keystore sem cadeia de certificados para o alias '" + al + "'");
            }
            log.info("Certificado de assinatura carregado ({}).",
                    certificado().getSubjectX500Principal().getName());
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível carregar o certificado de assinatura "
                    + "(assinatura.keystore): " + e.getClass().getSimpleName(), e);
        }
    }

    /** Construtor para testes, com chave e cadeia já resolvidas. */
    public CertificadoIcpBrasil(PrivateKey chavePrivada, Certificate[] cadeia) {
        this.chavePrivada = chavePrivada;
        this.cadeia = cadeia;
    }

    public PrivateKey chavePrivada() {
        return chavePrivada;
    }

    /** Certificado do titular (primeiro da cadeia). */
    public X509Certificate certificado() {
        return (X509Certificate) cadeia[0];
    }

    public Certificate[] cadeia() {
        return cadeia.clone();
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
