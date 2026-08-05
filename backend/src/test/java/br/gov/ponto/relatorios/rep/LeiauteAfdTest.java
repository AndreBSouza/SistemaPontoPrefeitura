package br.gov.ponto.relatorios.rep;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conformidade do AFD com o leiaute VIGENTE (versão "004", publicado no portal gov.br por força
 * do art. 81 da Portaria MTP 671/2021). Confere tamanhos e posições de campo caractere a
 * caractere — é o teste que impede uma regressão de leiaute passar despercebida.
 */
class LeiauteAfdTest {

    /** 2021-04-27T16:44:00-0300 é o exemplo de campo DH do próprio leiaute. */
    private static final Instant EXEMPLO_DO_LEIAUTE = Instant.parse("2021-04-27T19:44:00Z");

    private static String[] linhasDe(String afd) {
        assertThat(afd).endsWith("\r\n");
        return afd.split("\r\n", -1);
    }

    // ---------- CRC-16/KERMIT ----------

    @Test
    void crcUsaOVetorDeVerificacaoDoProprioLeiaute() {
        // "os 9 caracteres 123456789 geram o CRC-16 de valor 0x2189"
        assertThat(Crc16.hex("123456789", CampoLeiaute.CHARSET)).isEqualTo("2189");
    }

    // ---------- Formatação de campos ----------

    @Test
    void campoDhSegueOExemploDoLeiaute() {
        assertThat(CampoLeiaute.dh(EXEMPLO_DO_LEIAUTE)).isEqualTo("2021-04-27T16:44:00-0300");
        assertThat(CampoLeiaute.dh(EXEMPLO_DO_LEIAUTE)).hasSize(24);
    }

    @Test
    void campoDataUsaFormatoIso() {
        assertThat(CampoLeiaute.d(LocalDate.of(2026, 7, 1))).isEqualTo("2026-07-01").hasSize(10);
    }

    @Test
    void numericoAlinhaADireitaComZerosEAlfanumericoAEsquerdaComEspacos() {
        assertThat(CampoLeiaute.n(42, 9)).isEqualTo("000000042");
        assertThat(CampoLeiaute.a("ABC", 6)).isEqualTo("ABC   ");
    }

    @Test
    void arquivoEGravavelEmIso88591() {
        String texto = CampoLeiaute.a("Prefeitura de São João da Boa Vista", 60);
        // Round-trip sem perda: nada de caractere fora da ISO 8859-1 (que viraria "?" e deslocaria o CRC).
        assertThat(new String(texto.getBytes(CampoLeiaute.CHARSET), CampoLeiaute.CHARSET)).isEqualTo(texto);
    }

    // ---------- Registros ----------

    private MontadorAfd comCabecalho() {
        return new MontadorAfd().cabecalho(
                "11.222.333/0001-81", true, null, "Prefeitura Municipal de Exemplo",
                "BR512023000123456", "99.888.777/0001-66", true,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), EXEMPLO_DO_LEIAUTE);
    }

    @Test
    void cabecalhoTipo1TemAsPosicoesDoLeiaute() {
        String linha = linhasDe(comCabecalho().finalizar())[0];

        assertThat(linha).hasSize(302);
        assertThat(linha).as("campo 1 (001-009)").startsWith("000000000");
        assertThat(linha.charAt(9)).isEqualTo('1');                       // campo 2: tipo do registro
        assertThat(linha.charAt(10)).isEqualTo('1');                      // campo 3: 1 = CNPJ
        assertThat(linha.substring(11, 25)).isEqualTo("11222333000181"); // campo 4 (012-025)
        assertThat(linha.substring(25, 39)).isEqualTo("              "); // campo 5: CNO/CAEPF ausente
        assertThat(linha.substring(39, 189)).startsWith("Prefeitura Municipal de Exemplo");
        assertThat(linha.substring(189, 206)).isEqualTo("00512023000123456"); // campo 7: INPI (17 N)
        assertThat(linha.substring(206, 216)).isEqualTo("2026-07-01");    // campo 8: data inicial
        assertThat(linha.substring(216, 226)).isEqualTo("2026-07-31");    // campo 9: data final
        assertThat(linha.substring(226, 250)).isEqualTo("2021-04-27T16:44:00-0300"); // campo 10
        assertThat(linha.substring(250, 253)).isEqualTo("004");           // campo 11: versão
        assertThat(linha.charAt(253)).isEqualTo('1');                     // campo 12: desenvolvedor CNPJ
        assertThat(linha.substring(254, 268)).isEqualTo("99888777000166"); // campo 13
        assertThat(linha.substring(268, 298)).isBlank();                  // campo 14: modelo (só REP-C)
        assertThat(linha.substring(298, 302)).matches("[0-9A-F]{4}");     // campo 15: CRC-16
    }

    @Test
    void crcDoCabecalhoCobreOsCamposAnteriores() {
        String linha = linhasDe(comCabecalho().finalizar())[0];
        String semCrc = linha.substring(0, 298);
        assertThat(linha.substring(298)).isEqualTo(Crc16.hex(semCrc, CampoLeiaute.CHARSET));
    }

    @Test
    void empregadoTipo5TemAsPosicoesDoLeiaute() {
        String afd = comCabecalho()
                .empregado(1, EXEMPLO_DO_LEIAUTE, 'I', "123.456.789-01", "Joana da Silva", "98765432100")
                .finalizar();
        String linha = linhasDe(afd)[1];

        assertThat(linha).hasSize(118);
        assertThat(linha.substring(0, 9)).isEqualTo("000000001");
        assertThat(linha.charAt(9)).isEqualTo('5');
        assertThat(linha.substring(10, 34)).isEqualTo("2021-04-27T16:44:00-0300");
        assertThat(linha.charAt(34)).isEqualTo('I');
        assertThat(linha.substring(35, 47)).isEqualTo("012345678901"); // CPF em 12 posições
        assertThat(linha.substring(47, 99)).startsWith("Joana da Silva");
        assertThat(linha.substring(99, 103)).isBlank();
        assertThat(linha.substring(103, 114)).isEqualTo("98765432100");
        assertThat(linha.substring(114, 118)).matches("[0-9A-F]{4}");
    }

    @Test
    void marcacaoUsaOTipo7DoRepPNuncaOTipo3() {
        MontadorAfd m = comCabecalho();
        m.marcacao(2, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE,
                MontadorAfd.Coletor.APLICATIVO_MOBILE, false);
        String linha = linhasDe(m.finalizar())[1];

        assertThat(linha).hasSize(137);
        assertThat(linha.charAt(9))
                .as("REP-P grava marcação no tipo 7; o tipo 3 é de REP-C/REP-A")
                .isEqualTo('7');
        assertThat(linha.substring(10, 34)).isEqualTo("2021-04-27T16:44:00-0300"); // DH marcação
        assertThat(linha.substring(34, 46)).isEqualTo("012345678901");             // CPF
        assertThat(linha.substring(46, 70)).isEqualTo("2021-04-27T16:44:00-0300"); // DH gravação
        assertThat(linha.substring(70, 72)).isEqualTo("01");                       // coletor: mobile
        assertThat(linha.charAt(72)).isEqualTo('0');                               // on-line
        assertThat(linha.substring(73, 137)).matches("[0-9a-f]{64}");              // SHA-256
    }

    @Test
    void marcacaoOfflineMarcaOCampo7() {
        MontadorAfd m = comCabecalho();
        m.marcacao(2, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE,
                MontadorAfd.Coletor.APLICATIVO_MOBILE, true);
        assertThat(linhasDe(m.finalizar())[1].charAt(72)).isEqualTo('1');
    }

    @Test
    void hashDoTipo7EncadeiaComORegistroAnterior() {
        MontadorAfd m = comCabecalho();
        String h1 = m.marcacao(1, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE,
                MontadorAfd.Coletor.APLICATIVO_MOBILE, false);
        String h2 = m.marcacao(2, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE,
                MontadorAfd.Coletor.APLICATIVO_MOBILE, false);

        assertThat(h1).hasSize(64).isNotEqualTo(h2);

        // Mesmos campos, mas SEM o elo anterior → hash diferente: prova que o encadeamento entrou.
        String semElo = new MontadorAfd().cabecalho("11222333000181", true, null, "X", "1", "2", true,
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), EXEMPLO_DO_LEIAUTE)
                .marcacao(2, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE,
                        MontadorAfd.Coletor.APLICATIVO_MOBILE, false);
        assertThat(h2).isNotEqualTo(semElo);
    }

    @Test
    void eventoSensivelTipo6() {
        String afd = comCabecalho()
                .eventoSensivel(3, EXEMPLO_DO_LEIAUTE, MontadorAfd.EventoSensivel.INDISPONIBILIDADE_DE_SERVICO)
                .finalizar();
        String linha = linhasDe(afd)[1];

        assertThat(linha).hasSize(36);
        assertThat(linha.charAt(9)).isEqualTo('6');
        assertThat(linha.substring(34, 36)).isEqualTo("08");
    }

    @Test
    void trailerTipo9ContaPorTipoEFechaComO9NaPosicao64() {
        MontadorAfd m = comCabecalho();
        m.empregado(1, EXEMPLO_DO_LEIAUTE, 'I', "12345678901", "A", "98765432100");
        m.marcacao(2, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE, MontadorAfd.Coletor.BROWSER, false);
        m.marcacao(3, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE, MontadorAfd.Coletor.BROWSER, false);
        m.eventoSensivel(4, EXEMPLO_DO_LEIAUTE, MontadorAfd.EventoSensivel.DISPONIBILIDADE_DE_SERVICO);

        String[] linhas = linhasDe(m.finalizar());
        String trailer = linhas[linhas.length - 3]; // ..., trailer, assinatura, ""

        assertThat(trailer).hasSize(64);
        assertThat(trailer.substring(0, 9)).isEqualTo("999999999");
        assertThat(trailer.substring(9, 18)).isEqualTo("000000000");  // tipo 2 (não se aplica ao REP-P)
        assertThat(trailer.substring(18, 27)).isEqualTo("000000000"); // tipo 3 (REP-C/REP-A)
        assertThat(trailer.substring(27, 36)).isEqualTo("000000000"); // tipo 4
        assertThat(trailer.substring(36, 45)).isEqualTo("000000001"); // tipo 5
        assertThat(trailer.substring(45, 54)).isEqualTo("000000001"); // tipo 6
        assertThat(trailer.substring(54, 63)).isEqualTo("000000002"); // tipo 7
        assertThat(trailer.charAt(63)).isEqualTo('9');
    }

    @Test
    void ultimaLinhaEAMarcaDeAssinaturaEmArquivoP7s() {
        String[] linhas = linhasDe(comCabecalho().finalizar());
        String assinatura = linhas[linhas.length - 2];

        assertThat(assinatura).hasSize(100);
        assertThat(assinatura.strip()).isEqualTo("ASSINATURA_DIGITAL_EM_ARQUIVO_P7S");
    }

    @Test
    void arquivoNaoTemLinhasEmBrancoETerminaEmCrLf() {
        MontadorAfd m = comCabecalho();
        m.marcacao(1, EXEMPLO_DO_LEIAUTE, "12345678901", EXEMPLO_DO_LEIAUTE,
                MontadorAfd.Coletor.APLICATIVO_MOBILE, false);
        String afd = m.finalizar();

        assertThat(afd).endsWith("\r\n").doesNotContain("\r\n\r\n");
        assertThat(afd.replace("\r\n", "")).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void nomeDoArquivoSegueOPadraoDoRepP() {
        assertThat(MontadorAfd.nomeDoArquivo("BR51 2023 000123456", "11.222.333/0001-81"))
                .isEqualTo("AFD51202300012345611222333000181REP_P.txt");
    }
}
