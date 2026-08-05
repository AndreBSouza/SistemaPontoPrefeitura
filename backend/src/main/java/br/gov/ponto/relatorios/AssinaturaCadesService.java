package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.rep.CertificadoIcpBrasil;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Assinatura digital <b>CAdES/PKCS#7 (CMS)</b> do AFD e do AEJ com certificado <b>ICP-Brasil</b>
 * (arts. 86 e 88 da Portaria MTP 671/2021). Produz uma assinatura DESTACADA (detached) em DER,
 * devolvida em base64 — é o arquivo {@code .p7s} que acompanha o AFD, como o leiaute indica ao
 * exigir a marca "ASSINATURA_DIGITAL_EM_ARQUIVO_P7S" na última linha.
 *
 * <p>Bean {@code @Primary} registrado só quando há {@link CertificadoIcpBrasil} (ou seja, quando
 * {@code assinatura.keystore} está configurado). Sem certificado prevalece o
 * {@link AssinaturaIndisponivel} e os arquivos saem apenas com hash.</p>
 */
@Service
@Primary
@ConditionalOnBean(CertificadoIcpBrasil.class)
public class AssinaturaCadesService implements AssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaCadesService.class);

    private final CertificadoIcpBrasil certificado;

    public AssinaturaCadesService(CertificadoIcpBrasil certificado) {
        this.certificado = certificado;
        log.info("Assinatura CAdES habilitada para AFD/AEJ.");
    }

    @Override
    public Optional<String> assinar(byte[] conteudo) {
        try {
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build("SHA256withRSA", certificado.chavePrivada(), certificado.certificado()));
            gen.addCertificates(new JcaCertStore(List.of(certificado.cadeia())));
            // false = assinatura destacada (não encapsula o conteúdo do arquivo no .p7s).
            CMSSignedData assinado = gen.generate(new CMSProcessableByteArray(conteudo), false);
            return Optional.of(Base64.getEncoder().encodeToString(assinado.getEncoded()));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar em CAdES: " + e.getClass().getSimpleName(), e);
        }
    }
}
