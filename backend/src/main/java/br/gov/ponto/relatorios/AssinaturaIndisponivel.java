package br.gov.ponto.relatorios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementação default (dev / sem certificado): NÃO assina, apenas sinaliza no log.
 *
 * <p>Em produção, fornecer um bean {@code @Primary} de {@link AssinaturaService} que assine
 * em CAdES/PKCS#7 com certificado ICP-Brasil (e-CNPJ via keystore A1 ou HSM PKCS#11). Como o
 * ponto de plugue já existe, basta adicionar a implementação — nada mais muda. Ver
 * {@code docs/afd-assinatura-icp-brasil.md}.</p>
 */
@Service
public class AssinaturaIndisponivel implements AssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(AssinaturaIndisponivel.class);

    @Override
    public Optional<String> assinar(byte[] conteudo) {
        log.debug("Assinatura ICP-Brasil não configurada; AFD gerado sem assinatura digital (apenas hash).");
        return Optional.empty();
    }
}
