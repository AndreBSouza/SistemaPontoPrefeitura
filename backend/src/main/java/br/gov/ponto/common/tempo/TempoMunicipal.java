package br.gov.ponto.common.tempo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Fuso e janelas temporais do dominio municipal, em um unico lugar (evita ZoneId e
 * calculo de periodo duplicados — importante para a consistencia regulatoria do AFD).
 */
public final class TempoMunicipal {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private TempoMunicipal() {
    }

    /** Par [inicio, fim] do dia em Instant (fim = 23:59:59.999...). */
    public static Instant[] intervaloDoDia(LocalDate data) {
        return new Instant[]{
                data.atStartOfDay(ZONE).toInstant(),
                data.atTime(LocalTime.MAX).atZone(ZONE).toInstant(),
        };
    }

    /** Par [inicio, fim] da competencia (mes) em Instant. */
    public static Instant[] intervaloDaCompetencia(YearMonth competencia) {
        return new Instant[]{
                competencia.atDay(1).atStartOfDay(ZONE).toInstant(),
                competencia.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZONE).toInstant(),
        };
    }
}
