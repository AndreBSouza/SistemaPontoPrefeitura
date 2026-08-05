package br.gov.ponto.ativacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/** Geracao de codigos/tokens de ativacao e seus hashes (nunca guardamos o valor cru). */
public final class Tokens {

    private static final SecureRandom RNG = new SecureRandom();
    // Alfabeto sem caracteres ambiguos (0/O, 1/I) para o codigo ser ditado/digitado por idosos.
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private Tokens() {
    }

    /** Codigo legivel de 8 caracteres no formato XXXX-XXXX (ex.: ABCD-2345). */
    public static String gerarCodigoLegivel() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALFABETO.charAt(RNG.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** Normaliza para comparacao: remove tudo que nao for alfanumerico e coloca em maiusculas. */
    public static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    /** Token opaco do dispositivo (256 bits, base64url). */
    public static String gerarToken() {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** SHA-256 em hex (64 chars). */
    public static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
