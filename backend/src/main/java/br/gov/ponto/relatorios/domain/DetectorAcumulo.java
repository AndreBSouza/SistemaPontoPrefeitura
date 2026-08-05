package br.gov.ponto.relatorios.domain;

import java.util.List;

/**
 * Regra (POJO testável) de detecção de acúmulo ilícito de cargos: dois vínculos do
 * mesmo servidor cujas jornadas se sobrepõem no mesmo dia da semana — fisicamente
 * impossível cumprir ambos. Não usa biometria; cruza apenas as janelas de horário.
 */
public final class DetectorAcumulo {

    private DetectorAcumulo() {
    }

    /** Janela de trabalho esperada: dia da semana (ISO 1=seg..7=dom) + minutos do dia [inicio, fim). */
    public record Janela(int diaSemana, int inicioMinuto, int fimMinuto) {
    }

    /** Há conflito quando alguma janela de A colide com alguma de B no mesmo dia da semana. */
    public static boolean haConflito(List<Janela> a, List<Janela> b) {
        for (Janela ja : a) {
            for (Janela jb : b) {
                if (ja.diaSemana() == jb.diaSemana() && sobrepoe(ja, jb)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Dois intervalos [s,e) se sobrepõem quando começam antes de o outro terminar. */
    private static boolean sobrepoe(Janela x, Janela y) {
        return x.inicioMinuto() < y.fimMinuto() && y.inicioMinuto() < x.fimMinuto();
    }
}
