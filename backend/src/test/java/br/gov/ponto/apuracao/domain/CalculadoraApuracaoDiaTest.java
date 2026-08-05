package br.gov.ponto.apuracao.domain;

import br.gov.ponto.registro.domain.TipoMarcacao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Teste unitario puro (sem Spring/JPA) do nucleo de regra da apuracao. */
class CalculadoraApuracaoDiaTest {

    private final CalculadoraApuracaoDia calc = new CalculadoraApuracaoDia();

    private static Marcacao m(int hora, int min, TipoMarcacao tipo) {
        return new Marcacao(hora * 60 + min, tipo);
    }

    private int minutos(ResultadoApuracao r, TipoOcorrencia tipo) {
        return r.ocorrencias().stream().filter(o -> o.tipo() == tipo)
                .mapToInt(Ocorrencia::minutos).findFirst().orElse(-1);
    }

    @Test
    void atrasoAlemDaTolerancia() {
        var r = calc.calcular(
                List.of(m(8, 10, TipoMarcacao.ENTRADA), m(12, 0, TipoMarcacao.SAIDA)),
                8 * 60, 12 * 60, 5, 0);
        assertThat(r.minutosTrabalhados()).isEqualTo(230);
        assertThat(r.minutosEsperados()).isEqualTo(240);
        assertThat(minutos(r, TipoOcorrencia.ATRASO)).isEqualTo(10);
    }

    @Test
    void faltaQuandoSemMarcacoesEmDiaUtil() {
        var r = calc.calcular(List.of(), 8 * 60, 12 * 60, 5, 0);
        assertThat(r.ocorrencias()).extracting(Ocorrencia::tipo).containsExactly(TipoOcorrencia.FALTA);
        assertThat(minutos(r, TipoOcorrencia.FALTA)).isEqualTo(240);
    }

    @Test
    void horaExtra() {
        var r = calc.calcular(
                List.of(m(8, 0, TipoMarcacao.ENTRADA), m(13, 0, TipoMarcacao.SAIDA)),
                8 * 60, 12 * 60, 5, 0);
        assertThat(minutos(r, TipoOcorrencia.HORA_EXTRA)).isEqualTo(60);
    }

    @Test
    void saidaAntecipadaAlemDaTolerancia() {
        var r = calc.calcular(
                List.of(m(8, 0, TipoMarcacao.ENTRADA), m(11, 40, TipoMarcacao.SAIDA)),
                8 * 60, 12 * 60, 5, 0);
        assertThat(minutos(r, TipoOcorrencia.SAIDA_ANTECIPADA)).isEqualTo(20);
        assertThat(r.ocorrencias()).extracting(Ocorrencia::tipo).doesNotContain(TipoOcorrencia.ATRASO);
    }

    @Test
    void adicionalNoturnoEmTurnoNoturno() {
        // 22:00 -> 23:30 em dia sem expediente previsto (folga): tudo noturno + hora extra
        var r = calc.calcular(
                List.of(m(22, 0, TipoMarcacao.ENTRADA), m(23, 30, TipoMarcacao.SAIDA)),
                null, null, 0, 0);
        assertThat(minutos(r, TipoOcorrencia.ADICIONAL_NOTURNO)).isEqualTo(90);
        assertThat(r.diaUtil()).isFalse();
    }

    @Test
    void madrugadaContaComoNoturno() {
        var r = calc.calcular(
                List.of(m(0, 0, TipoMarcacao.ENTRADA), m(5, 0, TipoMarcacao.SAIDA)),
                null, null, 0, 0);
        assertThat(minutos(r, TipoOcorrencia.ADICIONAL_NOTURNO)).isEqualTo(300);
    }
}
