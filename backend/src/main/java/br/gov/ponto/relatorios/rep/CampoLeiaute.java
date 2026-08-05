package br.gov.ponto.relatorios.rep;

import br.gov.ponto.common.tempo.TempoMunicipal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Formatação dos tipos de campo definidos nos Anexos V (AFD) e VI (AEJ) da Portaria MTP 671/2021.
 *
 * <p>Tipos: <b>N</b> numérico, <b>A</b> alfanumérico, <b>D</b> data ({@code AAAA-MM-dd}),
 * <b>DH</b> data e hora ({@code AAAA-MM-ddThh:mm:00ZZZZZ}, com fuso) e <b>H</b> hora
 * ({@code hhmm}, só no AEJ).</p>
 *
 * <p>Os arquivos são texto em <b>ISO 8859-1</b> com linhas terminadas em CR+LF (itens 2 e 3 do
 * Anexo V) — daí a charset e o terminador ficarem aqui, num lugar só.</p>
 */
public final class CampoLeiaute {

    /** Codificação exigida pelo leiaute (ASCII da norma ISO 8859-1). */
    public static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    /** Terminador de linha exigido: caracteres 13 e 10 da tabela ASCII. */
    public static final String FIM_DE_LINHA = "\r\n";

    /** Segundos são fixos em "00" e o fuso sai como -0300 (ex.: 2021-04-27T16:44:00-0300). */
    private static final DateTimeFormatter DH =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:'00'ZZZ");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("uuuu-MM-dd");
    private static final DateTimeFormatter H = DateTimeFormatter.ofPattern("HHmm");

    private CampoLeiaute() {
    }

    /**
     * Campo numérico: só dígitos, alinhado à direita com zeros à esquerda.
     * Valor maior que o campo é truncado pela direita (mantém os dígitos mais significativos).
     */
    public static String n(long valor, int tamanho) {
        String s = Long.toString(Math.abs(valor));
        return s.length() >= tamanho ? s.substring(0, tamanho) : "0".repeat(tamanho - s.length()) + s;
    }

    /** Campo numérico a partir de um texto (extrai os dígitos; ex.: CNPJ com máscara). */
    public static String n(String valor, int tamanho) {
        String d = valor == null ? "" : valor.replaceAll("\\D", "");
        return d.length() >= tamanho ? d.substring(d.length() - tamanho) : "0".repeat(tamanho - d.length()) + d;
    }

    /**
     * Campo numérico OPCIONAL não informado: posições não utilizadas ficam em branco (item 7 do
     * Anexo V). Usado para CNO/CAEPF e afins quando o ente não possui o cadastro.
     */
    public static String branco(int tamanho) {
        return " ".repeat(tamanho);
    }

    /** Campo alfanumérico: começa pela esquerda, completa com espaços à direita. */
    public static String a(String valor, int tamanho) {
        String s = valor == null ? "" : semAcentoDeControle(valor);
        return s.length() >= tamanho ? s.substring(0, tamanho) : s + " ".repeat(tamanho - s.length());
    }

    /** Campo tipo D: data no fuso do município. */
    public static String d(LocalDate data) {
        return D.format(data);
    }

    /** Campo tipo D a partir de um instante. */
    public static String d(Instant instante) {
        return D.format(instante.atZone(TempoMunicipal.ZONE));
    }

    /** Campo tipo DH: data e hora com fuso (24 caracteres). */
    public static String dh(Instant instante) {
        return DH.format(instante.atZone(TempoMunicipal.ZONE));
    }

    /** Campo tipo H (AEJ): hora no formato hhmm. */
    public static String h(LocalTime hora) {
        return H.format(hora);
    }

    /**
     * Remove caracteres de controle e o que não existir em ISO 8859-1 (o arquivo é gravado nessa
     * codificação; um caractere fora dela viraria "?" e deslocaria a conferência do CRC).
     */
    private static String semAcentoDeControle(String valor) {
        StringBuilder sb = new StringBuilder(valor.length());
        for (char c : valor.toCharArray()) {
            if (c == '\r' || c == '\n' || c == '\t') {
                sb.append(' ');
            } else if (c <= 0xFF) {
                sb.append(c);
            } else {
                sb.append(' '); // fora do ISO 8859-1
            }
        }
        return sb.toString();
    }
}
