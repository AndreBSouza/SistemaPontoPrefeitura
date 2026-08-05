package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.api.BiExecutivoResponse;
import br.gov.ponto.relatorios.api.PrevisaoResponse;
import br.gov.ponto.relatorios.domain.ProjecaoAbsenteismo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Previsão de absenteísmo e de custo de hora extra (12.4.12), heurística: extrapola a série dos
 * últimos {@link #MESES_BASE} meses (faltas e minutos de HE do ente, via {@link BiExecutivoService}).
 */
@Service
public class PrevisaoService {

    private static final int MESES_BASE = 3;

    private final BiExecutivoService biExecutivoService;

    public PrevisaoService(BiExecutivoService biExecutivoService) {
        this.biExecutivoService = biExecutivoService;
    }

    @Transactional(readOnly = true)
    public PrevisaoResponse prever(YearMonth competencia, BigDecimal custoHora) {
        List<Integer> faltas = new ArrayList<>();
        List<Integer> horaExtra = new ArrayList<>();
        for (int i = MESES_BASE; i >= 1; i--) {
            BiExecutivoResponse bi = biExecutivoService.porOrgao(competencia.minusMonths(i));
            faltas.add(bi.totalFaltas());
            horaExtra.add(bi.totalHoraExtraMinutos());
        }

        long faltasProj = Math.round(ProjecaoAbsenteismo.projetar(faltas));
        int heMin = (int) Math.round(ProjecaoAbsenteismo.projetar(horaExtra));
        BigDecimal custo = (custoHora == null ? BigDecimal.ZERO : custoHora)
                .multiply(BigDecimal.valueOf(heMin).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);

        return new PrevisaoResponse(competencia.toString(), MESES_BASE, faltasProj, heMin, custo,
                ProjecaoAbsenteismo.rotuloTendencia(faltas, 0.5),
                ProjecaoAbsenteismo.rotuloTendencia(horaExtra, 30));
    }
}
