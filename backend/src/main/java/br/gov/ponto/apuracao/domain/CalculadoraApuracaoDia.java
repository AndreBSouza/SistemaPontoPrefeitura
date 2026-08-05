package br.gov.ponto.apuracao.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Nucleo de regra da apuracao diaria (stateless, sem Spring/JPA): a partir das
 * marcacoes (minutos do dia) e do horario esperado, calcula minutos trabalhados,
 * esperados e as ocorrencias (atraso, saida antecipada, falta, hora extra, adicional noturno).
 *
 * <p>Isolar esta aritmetica do {@code ApuracaoService} permite teste unitario rapido
 * (sem banco) das regras mais sujeitas a mudanca legal/sindical.</p>
 */
public final class CalculadoraApuracaoDia {

    private static final int NOITE_INICIO = 22 * 60;   // 22:00
    private static final int MADRUGADA_FIM = 5 * 60;   // 05:00

    public ResultadoApuracao calcular(List<Marcacao> marcacoes,
                                      Integer entradaEsperadaMin,
                                      Integer saidaEsperadaMin,
                                      int toleranciaMin,
                                      int intervaloMin) {
        List<int[]> intervalos = intervalosTrabalhados(marcacoes);
        int trabalhados = intervalos.stream().mapToInt(i -> i[1] - i[0]).sum();

        List<Ocorrencia> ocorrencias = new ArrayList<>();
        int noturno = intervalos.stream().mapToInt(this::minutosNoturnos).sum();
        if (noturno > 0) {
            ocorrencias.add(new Ocorrencia(TipoOcorrencia.ADICIONAL_NOTURNO, noturno));
        }

        boolean diaUtil = entradaEsperadaMin != null && saidaEsperadaMin != null;
        int esperados = 0;

        if (diaUtil) {
            esperados = Math.max(0, (saidaEsperadaMin - entradaEsperadaMin) - intervaloMin);
            if (intervalos.isEmpty()) {
                ocorrencias.add(new Ocorrencia(TipoOcorrencia.FALTA, esperados));
            } else {
                int entradaReal = intervalos.get(0)[0];
                int saidaReal = intervalos.get(intervalos.size() - 1)[1];
                int atraso = entradaReal - entradaEsperadaMin;
                if (atraso > toleranciaMin) {
                    ocorrencias.add(new Ocorrencia(TipoOcorrencia.ATRASO, atraso));
                }
                int antecipacao = saidaEsperadaMin - saidaReal;
                if (antecipacao > toleranciaMin) {
                    ocorrencias.add(new Ocorrencia(TipoOcorrencia.SAIDA_ANTECIPADA, antecipacao));
                }
                if (trabalhados > esperados) {
                    ocorrencias.add(new Ocorrencia(TipoOcorrencia.HORA_EXTRA, trabalhados - esperados));
                }
            }
        } else if (trabalhados > 0) {
            ocorrencias.add(new Ocorrencia(TipoOcorrencia.HORA_EXTRA, trabalhados));
        }

        return new ResultadoApuracao(trabalhados, esperados, diaUtil, ocorrencias);
    }

    private List<int[]> intervalosTrabalhados(List<Marcacao> marcacoes) {
        List<int[]> intervalos = new ArrayList<>();
        Integer inicioAtual = null;
        for (Marcacao m : marcacoes) {
            switch (m.tipo()) {
                case ENTRADA, INTERVALO_FIM -> {
                    if (inicioAtual == null) {
                        inicioAtual = m.minutoDoDia();
                    }
                }
                case SAIDA, INTERVALO_INICIO -> {
                    if (inicioAtual != null) {
                        intervalos.add(new int[]{inicioAtual, m.minutoDoDia()});
                        inicioAtual = null;
                    }
                }
            }
        }
        return intervalos;
    }

    private int minutosNoturnos(int[] intervalo) {
        return overlap(intervalo[0], intervalo[1], 0, MADRUGADA_FIM)
                + overlap(intervalo[0], intervalo[1], NOITE_INICIO, 24 * 60);
    }

    private int overlap(int aInicio, int aFim, int bInicio, int bFim) {
        return Math.max(0, Math.min(aFim, bFim) - Math.max(aInicio, bInicio));
    }
}
