package br.gov.ponto.relatorios;

import java.util.Optional;

/**
 * Assinatura digital do AFD/comprovantes. A implementação de produção assina em
 * CAdES/PKCS#7 com certificado <b>ICP-Brasil</b> (e-CNPJ — keystore A1 ou HSM PKCS#11);
 * ver {@code docs/afd-assinatura-icp-brasil.md}.
 */
public interface AssinaturaService {

    /** Retorna a assinatura (base64) do conteúdo, ou vazio se não houver certificado configurado. */
    Optional<String> assinar(byte[] conteudo);
}
