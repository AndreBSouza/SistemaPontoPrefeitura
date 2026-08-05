package br.gov.ponto.relatorios.rep;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;

import static br.gov.ponto.relatorios.rep.CampoLeiaute.FIM_DE_LINHA;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.a;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.branco;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.d;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.dh;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.n;

/**
 * Monta o <b>AFD (Arquivo-Fonte de Dados)</b> de um <b>REP-P</b> conforme o Anexo V da Portaria
 * MTP 671/2021 — leiaute de largura fixa, versão "004".
 *
 * <p>Registros gerados: tipo 1 (cabeçalho), tipo 5 (empregados), tipo 6 (eventos sensíveis),
 * tipo 7 (marcações de ponto do REP-P), tipo 9 (trailer) e a linha de assinatura digital.
 * Os tipos 2 (identificação da empresa no REP), 3 (marcação de REP-C/REP-A) e 4 (ajuste de
 * relógio) não se aplicam a este produto e ficam zerados no trailer.</p>
 *
 * <p><b>Integridade:</b> os registros dos tipos 1 a 5 levam CRC-16/KERMIT; o tipo 7 leva o
 * SHA-256 encadeado com o hash do registro anterior (campo 8).</p>
 *
 * <p>Classe PURA (sem Spring/banco) para ser verificável byte a byte nos testes.</p>
 */
public class MontadorAfd {

    /** Versão do leiaute do AFD (campo 11 do cabeçalho). */
    public static final String VERSAO_LEIAUTE = "004";

    /** Linha final: no REP-P a assinatura vai em arquivo .p7s separado (Anexo V). */
    public static final String MARCA_ASSINATURA = "ASSINATURA_DIGITAL_EM_ARQUIVO_P7S";

    /** Identificador do coletor da marcação (campo 6 do registro tipo 7). */
    public enum Coletor {
        APLICATIVO_MOBILE("01"),
        BROWSER("02"),
        APLICATIVO_DESKTOP("03"),
        DISPOSITIVO_ELETRONICO("04"),
        OUTRO("05");

        private final String codigo;

        Coletor(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    /** Tipos de evento sensível aplicáveis ao REP-P (campo 4 do registro tipo 6). */
    public enum EventoSensivel {
        RETORNO_DE_ENERGIA("02"),
        DISPONIBILIDADE_DE_SERVICO("07"),
        INDISPONIBILIDADE_DE_SERVICO("08");

        private final String codigo;

        EventoSensivel(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    private final StringBuilder conteudo = new StringBuilder();
    private int qtdTipo2;
    private int qtdTipo3;
    private int qtdTipo4;
    private int qtdTipo5;
    private int qtdTipo6;
    private int qtdTipo7;
    private String hashAnterior = "";
    private boolean finalizado;

    /**
     * Registro tipo "1" — cabeçalho (302 caracteres).
     *
     * @param cnpjEmpregador   CNPJ (14) ou CPF (11) do ente
     * @param empregadorEhCnpj true = CNPJ, false = CPF
     * @param cnoOuCaepf       CNO/CAEPF quando existir; nulo/vazio grava em branco
     * @param razaoSocial      razão social ou nome do empregador
     * @param registroInpi     número de registro do REP-P no INPI (art. 91)
     * @param cnpjDesenvolvedor CNPJ/CPF do desenvolvedor do REP-P
     */
    public MontadorAfd cabecalho(String cnpjEmpregador, boolean empregadorEhCnpj, String cnoOuCaepf,
                                 String razaoSocial, String registroInpi,
                                 String cnpjDesenvolvedor, boolean desenvolvedorEhCnpj,
                                 LocalDate dataInicial, LocalDate dataFinal, Instant geradoEm) {
        String semCrc = n(0, 9)
                + "1"
                + (empregadorEhCnpj ? "1" : "2")
                + a(soDigitos(cnpjEmpregador), 14)
                + (vazio(cnoOuCaepf) ? branco(14) : n(cnoOuCaepf, 14))
                + a(razaoSocial, 150)
                + n(registroInpi, 17)
                + d(dataInicial)
                + d(dataFinal)
                + dh(geradoEm)
                + VERSAO_LEIAUTE
                + (desenvolvedorEhCnpj ? "1" : "2")
                + a(soDigitos(cnpjDesenvolvedor), 14)
                + a("", 30); // modelo: só para REP-C
        linha(semCrc + Crc16.hex(semCrc, CampoLeiaute.CHARSET));
        return this;
    }

    /**
     * Registro tipo "5" — inclusão/alteração/exclusão de empregado (118 caracteres).
     *
     * @param operacao 'I' inclusão, 'A' alteração, 'E' exclusão
     */
    public MontadorAfd empregado(long nsr, Instant gravadoEm, char operacao, String cpf, String nome,
                                 String cpfResponsavel) {
        String semCrc = n(nsr, 9)
                + "5"
                + dh(gravadoEm)
                + a(String.valueOf(operacao), 1)
                + n(cpf, 12)
                + a(nome, 52)
                + a("", 4) // demais dados de identificação
                + (vazio(cpfResponsavel) ? branco(11) : n(cpfResponsavel, 11));
        linha(semCrc + Crc16.hex(semCrc, CampoLeiaute.CHARSET));
        qtdTipo5++;
        return this;
    }

    /** Registro tipo "6" — evento sensível do REP (36 caracteres; sem CRC, fora da faixa 1–5). */
    public MontadorAfd eventoSensivel(long nsr, Instant gravadoEm, EventoSensivel evento) {
        linha(n(nsr, 9) + "6" + dh(gravadoEm) + evento.codigo());
        qtdTipo6++;
        return this;
    }

    /**
     * Registro tipo "7" — marcação de ponto do REP-P (137 caracteres).
     *
     * <p>O campo 8 é o SHA-256 calculado sobre os campos 1 a 7 <b>mais o hash do registro
     * anterior</b> (encadeamento), na ordem definida pelo Anexo V.</p>
     *
     * @param offline true quando a marcação foi coletada sem conexão (campo 7 = "1")
     * @return o hash gravado (útil para o comprovante do trabalhador — art. 79, VIII)
     */
    public String marcacao(long nsr, Instant dataHoraMarcacao, String cpf, Instant dataHoraGravacao,
                           Coletor coletor, boolean offline) {
        return marcacao(nsr, dataHoraMarcacao, cpf, dataHoraGravacao, coletor, offline, null);
    }

    /**
     * Igual ao anterior, mas usando um hash JÁ CALCULADO na hora da batida (o que foi mostrado ao
     * trabalhador no comprovante, art. 79, VIII). Passar o hash gravado garante que comprovante e
     * AFD exibam exatamente o mesmo valor. Com {@code null}, calcula encadeando dentro do arquivo.
     */
    public String marcacao(long nsr, Instant dataHoraMarcacao, String cpf, Instant dataHoraGravacao,
                           Coletor coletor, boolean offline, String hashGravado) {
        String hash = (hashGravado == null || hashGravado.isBlank())
                ? HashMarcacaoRep.calcular(nsr, dataHoraMarcacao, cpf, dataHoraGravacao, coletor,
                        offline, hashAnterior)
                : hashGravado;
        linha(n(nsr, 9) + "7" + dh(dataHoraMarcacao) + n(cpf, 12) + dh(dataHoraGravacao)
                + coletor.codigo() + (offline ? "1" : "0") + a(hash, 64));
        hashAnterior = hash;
        qtdTipo7++;
        return hash;
    }

    /**
     * Fecha o arquivo com o registro tipo "9" (trailer, 64 caracteres) e a linha de assinatura
     * digital (100 caracteres).
     */
    public String finalizar() {
        if (finalizado) {
            throw new IllegalStateException("AFD já finalizado");
        }
        finalizado = true;
        linha(n(999999999L, 9)
                + n(qtdTipo2, 9)
                + n(qtdTipo3, 9)
                + n(qtdTipo4, 9)
                + n(qtdTipo5, 9)
                + n(qtdTipo6, 9)
                + n(qtdTipo7, 9)
                + "9");
        linha(a(MARCA_ASSINATURA, 100));
        return conteudo.toString();
    }

    public int quantidadeDeMarcacoes() {
        return qtdTipo7;
    }

    /**
     * Nome do arquivo exigido pelo Anexo V para o REP-P: "AFD" + número de registro no INPI +
     * CNPJ/CPF do empregador + "REP_P".
     */
    public static String nomeDoArquivo(String registroInpi, String cnpjEmpregador) {
        return "AFD" + soDigitos(registroInpi) + soDigitos(cnpjEmpregador) + "REP_P.txt";
    }

    private void linha(String registro) {
        conteudo.append(registro).append(FIM_DE_LINHA);
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String soDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private static String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(CampoLeiaute.CHARSET));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
