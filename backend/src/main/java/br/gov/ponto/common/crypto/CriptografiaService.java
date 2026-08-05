package br.gov.ponto.common.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Criptografia simetrica de campos sensiveis em repouso (AES-256-GCM). A chave (256 bits)
 * e derivada por SHA-256 do segredo configurado ({@code ponto.crypto.secret} / env
 * {@code PONTO_CRYPTO_SECRET}) — em producao, use um segredo forte fora do repositorio.
 * Cada cifragem usa IV aleatorio; o IV e prefixado ao texto cifrado e o todo vai em base64.
 */
@Service
public class CriptografiaService {

    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int IV_TAMANHO = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom rng = new SecureRandom();

    public CriptografiaService(
            @Value("${ponto.crypto.secret:dev-secret-ponto-municipal-trocar-em-producao}") String segredo) {
        try {
            byte[] material = MessageDigest.getInstance("SHA-256")
                    .digest(segredo.getBytes(StandardCharsets.UTF_8));
            this.chave = new SecretKeySpec(material, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }

    /** Cifra e retorna base64(iv || ciphertext+tag). */
    public String cifrar(String texto) {
        try {
            byte[] iv = new byte[IV_TAMANHO];
            rng.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, saida, 0, iv.length);
            System.arraycopy(cifrado, 0, saida, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar", e);
        }
    }

    /** Decifra um valor produzido por {@link #cifrar(String)}. */
    public String decifrar(String base64) {
        try {
            byte[] entrada = Base64.getDecoder().decode(base64);
            byte[] iv = Arrays.copyOfRange(entrada, 0, IV_TAMANHO);
            byte[] cifrado = Arrays.copyOfRange(entrada, IV_TAMANHO, entrada.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao decifrar", e);
        }
    }
}
