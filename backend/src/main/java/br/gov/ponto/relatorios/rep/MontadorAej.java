package br.gov.ponto.relatorios.rep;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static br.gov.ponto.relatorios.rep.CampoLeiaute.FIM_DE_LINHA;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.d;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.dh;
import static br.gov.ponto.relatorios.rep.CampoLeiaute.h;

/**
 * Monta o <b>AEJ (Arquivo Eletrônico de Jornada)</b>, a saída do Programa de Tratamento de
 * Registro de Ponto (PTRP), conforme o leiaute <b>versão "002"</b> publicado no portal gov.br
 * (art. 83, I, da Portaria MTP 671/2021 — o Anexo VI original foi revogado pela Portaria MTP
 * 1.486/2022 e o leiaute passou a ser mantido fora da portaria).
 *
 * <p>Diferente do AFD, o AEJ é <b>delimitado por "|"</b> (não é largura fixa) e os campos têm
 * tamanho variável; o pipe separa os campos e NÃO aparece após o último campo do registro.</p>
 *
 * <p>Classe PURA (sem Spring/banco) para ser verificável nos testes.</p>
 */
public class MontadorAej {

    /** Versão do leiaute do AEJ (campo 10 do cabeçalho). */
    public static final String VERSAO_LEIAUTE = "002";

    /** Linha final: a assinatura vai em arquivo .p7s separado. */
    public static final String MARCA_ASSINATURA = MontadorAfd.MARCA_ASSINATURA;

    /** Tipo da marcação (campo 5 do registro "05"). */
    public enum TipoMarc {
        ENTRADA("E"), SAIDA("S"), DESCONSIDERADA("D");

        private final String codigo;

        TipoMarc(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    /** Fonte da marcação (campo 7 do registro "05"). */
    public enum FonteMarc {
        ORIGINAL_DO_REP("O"),
        INCLUIDA_MANUALMENTE("I"),
        PRE_ASSINALADA("P"),
        PONTO_POR_EXCECAO("X"),
        OUTRAS("T");

        private final String codigo;

        FonteMarc(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    /** Tipo de ausência ou compensação (campo 3 do registro "07"). */
    public enum TipoAusencia {
        DSR("1"), FALTA_NAO_JUSTIFICADA("2"), BANCO_DE_HORAS("3"), FOLGA_COMPENSATORIA_FERIADO("4");

        private final String codigo;

        TipoAusencia(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    /** Movimento no banco de horas (campo 6 do registro "07"). */
    public enum MovimentoBancoHoras {
        INCLUSAO("1"), COMPENSACAO("2");

        private final String codigo;

        MovimentoBancoHoras(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    private final StringBuilder conteudo = new StringBuilder();
    private final int[] quantidades = new int[9]; // índices 1..8 = tipos "01".."08"
    private boolean finalizado;

    /** Registro "01" — cabeçalho. */
    public MontadorAej cabecalho(String idEmpregador, boolean empregadorEhCnpj, String caepf, String cno,
                                 String razaoOuNome, LocalDate dataInicial, LocalDate dataFinal,
                                 Instant geradoEm) {
        registro(1,
                "01",
                empregadorEhCnpj ? "1" : "2",
                soDigitos(idEmpregador),
                soDigitos(caepf),
                soDigitos(cno),
                texto(razaoOuNome, 150),
                d(dataInicial),
                d(dataFinal),
                dh(geradoEm),
                VERSAO_LEIAUTE);
        return this;
    }

    /** Registro "02" — REP utilizado. Para este produto, sempre REP-P (tipo "3"). */
    public MontadorAej repP(int idRepAej, String registroInpi) {
        registro(2, "02", String.valueOf(idRepAej), "3", soDigitos(registroInpi));
        return this;
    }

    /** Registro "03" — vínculo (empregado). */
    public MontadorAej vinculo(int idtVinculoAej, String cpf, String nome) {
        registro(3, "03", String.valueOf(idtVinculoAej), soDigitos(cpf), texto(nome, 150));
        return this;
    }

    /**
     * Registro "04" — horário contratual.
     *
     * @param duracaoMinutos duração da jornada convertida em minutos (com redução noturna, se houver)
     * @param pares          pares entrada/saída em ordem; cada par vira hrEntradaNN/hrSaidaNN
     */
    public MontadorAej horarioContratual(String codigo, int duracaoMinutos,
                                         List<ParEntradaSaida> pares) {
        List<String> campos = new ArrayList<>(List.of(
                "04", texto(codigo, 30), String.valueOf(duracaoMinutos)));
        for (ParEntradaSaida p : pares) {
            campos.add(h(p.entrada()));
            campos.add(h(p.saida()));
        }
        // O leiaute prevê ao menos um par (hrEntrada01/hrSaida01) mesmo quando não informado.
        if (pares.isEmpty()) {
            campos.add("");
            campos.add("");
        }
        registro(4, campos.toArray(String[]::new));
        return this;
    }

    /** Par entrada/saída de um horário contratual. */
    public record ParEntradaSaida(LocalTime entrada, LocalTime saida) {
    }

    /**
     * Registro "05" — marcação.
     *
     * @param idRepAej          id do REP (registro "02"); vazio quando a marcação não veio do REP
     * @param seqEntSaida       número sequencial do par entrada/saída no dia
     * @param codHorContratual  obrigatório quando é a primeira entrada (tpMarc "E" e seq 1)
     * @param motivo            obrigatório quando desconsiderada ou incluída manualmente
     */
    public MontadorAej marcacao(int idtVinculoAej, Instant dataHoraMarcacao, Integer idRepAej,
                                TipoMarc tipo, int seqEntSaida, FonteMarc fonte,
                                String codHorContratual, String motivo) {
        registro(5,
                "05",
                String.valueOf(idtVinculoAej),
                dh(dataHoraMarcacao),
                idRepAej == null ? "" : String.valueOf(idRepAej),
                tipo.codigo(),
                CampoLeiaute.n(seqEntSaida, 3),
                fonte.codigo(),
                texto(codHorContratual, 30),
                texto(motivo, 150));
        return this;
    }

    /** Registro "06" — matrícula do vínculo no eSocial (para quem tem mais de um vínculo). */
    public MontadorAej matriculaEsocial(int idtVinculoAej, String matricula) {
        registro(6, "06", String.valueOf(idtVinculoAej), texto(matricula, 30));
        return this;
    }

    /** Registro "07" — ausência ou movimento de banco de horas. */
    public MontadorAej ausenciaOuBancoDeHoras(int idtVinculoAej, TipoAusencia tipo, LocalDate data,
                                              Integer qtMinutos, MovimentoBancoHoras movimento) {
        registro(7,
                "07",
                String.valueOf(idtVinculoAej),
                tipo.codigo(),
                d(data),
                qtMinutos == null ? "" : String.valueOf(Math.abs(qtMinutos)),
                movimento == null ? "" : movimento.codigo());
        return this;
    }

    /** Registro "08" — identificação do Programa de Tratamento de Registro de Ponto. */
    public MontadorAej identificacaoPtrp(String nomePrograma, String versaoPrograma,
                                         boolean desenvolvedorEhCnpj, String idDesenvolvedor,
                                         String razaoDesenvolvedor, String emailDesenvolvedor) {
        registro(8,
                "08",
                texto(nomePrograma, 150),
                texto(versaoPrograma, 8),
                desenvolvedorEhCnpj ? "1" : "2",
                soDigitos(idDesenvolvedor),
                texto(razaoDesenvolvedor, 150),
                texto(emailDesenvolvedor, 50));
        return this;
    }

    /** Fecha o arquivo com o trailer "99" e a linha de assinatura digital. */
    public String finalizar() {
        if (finalizado) {
            throw new IllegalStateException("AEJ já finalizado");
        }
        finalizado = true;
        // O trailer NÃO se conta a si mesmo; as quantidades são dos tipos "01" a "08".
        linha(String.join("|", "99",
                String.valueOf(quantidades[1]), String.valueOf(quantidades[2]),
                String.valueOf(quantidades[3]), String.valueOf(quantidades[4]),
                String.valueOf(quantidades[5]), String.valueOf(quantidades[6]),
                String.valueOf(quantidades[7]), String.valueOf(quantidades[8])));
        linha(CampoLeiaute.a(MARCA_ASSINATURA, 100));
        return conteudo.toString();
    }

    public int quantidadeDeMarcacoes() {
        return quantidades[5];
    }

    private void registro(int tipo, String... campos) {
        quantidades[tipo]++;
        linha(String.join("|", campos));
    }

    private void linha(String registro) {
        conteudo.append(registro).append(FIM_DE_LINHA);
    }

    /** Campo alfanumérico de tamanho VARIÁVEL: sem preenchimento, só o teto do leiaute. */
    private static String texto(String valor, int maximo) {
        if (valor == null) {
            return "";
        }
        String limpo = valor.replace('|', ' ').replace('\r', ' ').replace('\n', ' ');
        return limpo.length() > maximo ? limpo.substring(0, maximo) : limpo;
    }

    private static String soDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
