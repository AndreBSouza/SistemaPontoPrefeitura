package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.api.BiExecutivoResponse;
import br.gov.ponto.relatorios.api.RoiResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Painel de ROI / economia (12.3.1): a partir do apurado (hora extra e faltas do BI) e dos
 * percentuais de redução informados pelo gestor, estima a economia em R$ — "o sistema se paga".
 */
@Service
public class RoiService {

    private final BiExecutivoService biExecutivoService;

    public RoiService(BiExecutivoService biExecutivoService) {
        this.biExecutivoService = biExecutivoService;
    }

    @Transactional(readOnly = true)
    public RoiResponse estimar(YearMonth competencia, BigDecimal custoHoraReais, BigDecimal reducaoHoraExtraPct,
                               BigDecimal custoDiaFaltaReais, BigDecimal reducaoAbsenteismoPct) {
        BiExecutivoResponse bi = biExecutivoService.porOrgao(competencia);

        BigDecimal horasExtra = BigDecimal.valueOf(bi.totalHoraExtraMinutos())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal custoHE = horasExtra.multiply(nz(custoHoraReais)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal economiaHE = custoHE.multiply(fracao(reducaoHoraExtraPct)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoAbs = BigDecimal.valueOf(bi.totalFaltas())
                .multiply(nz(custoDiaFaltaReais)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal economiaAbs = custoAbs.multiply(fracao(reducaoAbsenteismoPct)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal economiaTotal = economiaHE.add(economiaAbs);
        return new RoiResponse(competencia.toString(), custoHE, economiaHE, custoAbs, economiaAbs, economiaTotal);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal fracao(BigDecimal pct) {
        return nz(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }
}
