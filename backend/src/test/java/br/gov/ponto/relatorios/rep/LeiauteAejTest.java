package br.gov.ponto.relatorios.rep;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conformidade do AEJ com o leiaute VIGENTE (versão "002", publicado no portal gov.br por força
 * do art. 83, I, da Portaria MTP 671/2021 — o Anexo VI original foi revogado em 2022).
 *
 * <p>Ao contrário do AFD, o AEJ é delimitado por "|" e tem campos de tamanho variável.</p>
 */
class LeiauteAejTest {

    private static final Instant EXEMPLO_DO_LEIAUTE = Instant.parse("2021-04-27T19:44:00Z");

    private static String[] linhasDe(String aej) {
        assertThat(aej).endsWith("\r\n");
        return aej.split("\r\n", -1);
    }

    private MontadorAej comCabecalho() {
        return new MontadorAej().cabecalho("11.222.333/0001-81", true, null, null,
                "Prefeitura Municipal de Exemplo",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), EXEMPLO_DO_LEIAUTE);
    }

    @Test
    void cabecalhoTipo01UsaVersao002() {
        String[] campos = linhasDe(comCabecalho().finalizar())[0].split("\\|", -1);

        assertThat(campos).hasSize(10);
        assertThat(campos[0]).isEqualTo("01");
        assertThat(campos[1]).isEqualTo("1");                    // CNPJ
        assertThat(campos[2]).isEqualTo("11222333000181");
        assertThat(campos[3]).isEmpty();                          // caepf
        assertThat(campos[4]).isEmpty();                          // cno
        assertThat(campos[5]).isEqualTo("Prefeitura Municipal de Exemplo");
        assertThat(campos[6]).isEqualTo("2026-07-01");
        assertThat(campos[7]).isEqualTo("2026-07-31");
        assertThat(campos[8]).isEqualTo("2021-04-27T16:44:00-0300");
        assertThat(campos[9]).as("leiaute vigente do AEJ").isEqualTo("002");
    }

    @Test
    void repUtilizadoEDoTipo3RepP() {
        String[] campos = linhasDe(comCabecalho().repP(1, "BR512023000123456").finalizar())[1]
                .split("\\|", -1);

        assertThat(campos[0]).isEqualTo("02");
        assertThat(campos[1]).isEqualTo("1");
        assertThat(campos[2]).as("3 = REP-P").isEqualTo("3");
        assertThat(campos[3]).isEqualTo("512023000123456");
    }

    @Test
    void vinculoEHorarioContratual() {
        MontadorAej m = comCabecalho()
                .vinculo(1, "123.456.789-01", "Joana da Silva")
                .horarioContratual("J40", 480, List.of(
                        new MontadorAej.ParEntradaSaida(LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new MontadorAej.ParEntradaSaida(LocalTime.of(13, 0), LocalTime.of(17, 0))));
        String[] linhas = linhasDe(m.finalizar());

        assertThat(linhas[1].split("\\|", -1))
                .containsExactly("03", "1", "12345678901", "Joana da Silva");
        assertThat(linhas[2].split("\\|", -1))
                .containsExactly("04", "J40", "480", "0800", "1200", "1300", "1700");
    }

    @Test
    void marcacaoOriginalDoRepEMarcacaoIncluidaManualmente() {
        MontadorAej m = comCabecalho();
        m.marcacao(1, EXEMPLO_DO_LEIAUTE, 1, MontadorAej.TipoMarc.ENTRADA, 1,
                MontadorAej.FonteMarc.ORIGINAL_DO_REP, "J40", null);
        m.marcacao(1, EXEMPLO_DO_LEIAUTE, null, MontadorAej.TipoMarc.SAIDA, 1,
                MontadorAej.FonteMarc.INCLUIDA_MANUALMENTE, null, "Esqueceu de bater — aprovado pela chefia");
        String[] linhas = linhasDe(m.finalizar());

        assertThat(linhas[1].split("\\|", -1))
                .containsExactly("05", "1", "2021-04-27T16:44:00-0300", "1", "E", "001", "O", "J40", "");
        // Marcação incluída manualmente: sem REP de origem e COM motivo (obrigatório).
        String[] incluida = linhas[2].split("\\|", -1);
        assertThat(incluida[3]).isEmpty();
        assertThat(incluida[6]).isEqualTo("I");
        assertThat(incluida[8]).isEqualTo("Esqueceu de bater — aprovado pela chefia");
    }

    @Test
    void ausenciaEMovimentoDeBancoDeHoras() {
        MontadorAej m = comCabecalho();
        m.ausenciaOuBancoDeHoras(1, MontadorAej.TipoAusencia.FALTA_NAO_JUSTIFICADA,
                LocalDate.of(2026, 7, 10), null, null);
        m.ausenciaOuBancoDeHoras(1, MontadorAej.TipoAusencia.BANCO_DE_HORAS,
                LocalDate.of(2026, 7, 11), 120, MontadorAej.MovimentoBancoHoras.INCLUSAO);
        String[] linhas = linhasDe(m.finalizar());

        assertThat(linhas[1].split("\\|", -1)).containsExactly("07", "1", "2", "2026-07-10", "", "");
        assertThat(linhas[2].split("\\|", -1)).containsExactly("07", "1", "3", "2026-07-11", "120", "1");
    }

    @Test
    void identificacaoDoPtrp() {
        String linha = linhasDe(comCabecalho()
                .identificacaoPtrp("Ponto Municipal", "1.0.0", true, "99.888.777/0001-66",
                        "Fornecedor de Software LTDA", "suporte@fornecedor.com.br")
                .finalizar())[1];

        assertThat(linha.split("\\|", -1)).containsExactly("08", "Ponto Municipal", "1.0.0", "1",
                "99888777000166", "Fornecedor de Software LTDA", "suporte@fornecedor.com.br");
    }

    @Test
    void trailerContaOsOitoTipos() {
        MontadorAej m = comCabecalho();
        m.repP(1, "1");
        m.vinculo(1, "12345678901", "A");
        m.horarioContratual("J40", 480, List.of(
                new MontadorAej.ParEntradaSaida(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        m.marcacao(1, EXEMPLO_DO_LEIAUTE, 1, MontadorAej.TipoMarc.ENTRADA, 1,
                MontadorAej.FonteMarc.ORIGINAL_DO_REP, "J40", null);
        m.marcacao(1, EXEMPLO_DO_LEIAUTE, 1, MontadorAej.TipoMarc.SAIDA, 1,
                MontadorAej.FonteMarc.ORIGINAL_DO_REP, null, null);
        m.matriculaEsocial(1, "M-8");
        m.ausenciaOuBancoDeHoras(1, MontadorAej.TipoAusencia.DSR, LocalDate.of(2026, 7, 5), null, null);
        m.identificacaoPtrp("Ponto Municipal", "1.0.0", true, "99888777000166", "F", "e@f.com");

        String[] linhas = linhasDe(m.finalizar());
        String trailer = linhas[linhas.length - 3];

        assertThat(trailer.split("\\|", -1))
                .containsExactly("99", "1", "1", "1", "1", "2", "1", "1", "1");
    }

    @Test
    void ultimaLinhaEAMarcaDeAssinaturaEmArquivoP7s() {
        String[] linhas = linhasDe(comCabecalho().finalizar());
        String assinatura = linhas[linhas.length - 2];

        assertThat(assinatura).hasSize(100);
        assertThat(assinatura.strip()).isEqualTo("ASSINATURA_DIGITAL_EM_ARQUIVO_P7S");
    }

    @Test
    void pipeNoConteudoNaoQuebraOsCampos() {
        // Nome com "|" corromperia a contagem de campos do arquivo inteiro.
        String linha = linhasDe(comCabecalho().vinculo(1, "12345678901", "Maria | Souza").finalizar())[1];
        assertThat(linha.split("\\|", -1)).hasSize(4);
    }
}
