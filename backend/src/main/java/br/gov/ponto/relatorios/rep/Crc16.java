package br.gov.ponto.relatorios.rep;

import java.nio.charset.Charset;

/**
 * CRC-16/KERMIT (também chamado CCITT-TRUE), exigido nos registros dos tipos "1" a "5" do AFD
 * gerado por REP-A/REP-P (Anexo V da Portaria MTP 671/2021).
 *
 * <p>Parâmetros: polinômio 0x1021 refletido (0x8408), init 0x0000, entrada e saída refletidas,
 * sem XOR final. O próprio anexo fornece o vetor de verificação: os 9 caracteres "123456789"
 * produzem 0x2189 — coberto por teste.</p>
 */
public final class Crc16 {

    private static final int POLINOMIO_REFLETIDO = 0x8408;

    private Crc16() {
    }

    /** CRC-16/KERMIT dos bytes informados. */
    public static int calcular(byte[] dados) {
        int crc = 0x0000;
        for (byte b : dados) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                boolean bitBaixo = (crc & 1) != 0;
                crc >>>= 1;
                if (bitBaixo) {
                    crc ^= POLINOMIO_REFLETIDO;
                }
            }
        }
        return crc & 0xFFFF;
    }

    /**
     * CRC-16 do texto, na forma exigida pelo AFD: 4 caracteres hexadecimais MAIÚSCULOS, sem o
     * prefixo "0x" (ex.: "2189").
     */
    public static String hex(String texto, Charset charset) {
        return String.format("%04X", calcular(texto.getBytes(charset)));
    }
}
