package br.gov.ponto.relatorios;

import br.gov.ponto.relatorios.api.SimulacaoResponse;
import br.gov.ponto.relatorios.domain.LimiteLrf;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simulador orçamentário de hora extra (12.5.5) + alerta de limite da LRF (12.5.1).
 * Cálculo puro (sem persistência); a aritmética do limite vive em {@link LimiteLrf}.
 */
@Service
public class SimuladorService {

    public SimulacaoResponse simular(BigDecimal horasExtras, BigDecimal custoHoraReais,
                                     BigDecimal gastoPessoalAtualReais, BigDecimal rclReais) {
        BigDecimal horas = nz(horasExtras);
        BigDecimal custoHora = nz(custoHoraReais);
        BigDecimal gastoAtual = nz(gastoPessoalAtualReais);

        BigDecimal custoHoraExtra = horas.multiply(custoHora).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gastoProjetado = gastoAtual.add(custoHoraExtra).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentual = LimiteLrf.percentualSobreRcl(gastoAtual, custoHoraExtra, rclReais);
        LimiteLrf.Risco risco = LimiteLrf.classificar(percentual);

        BigDecimal limite = LimiteLrf.LIMITE_EXECUTIVO.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal prudencial = limite.multiply(LimiteLrf.FATOR_PRUDENCIAL).setScale(2, RoundingMode.HALF_UP);

        return new SimulacaoResponse(custoHoraExtra, gastoProjetado, percentual, risco.name(), limite, prudencial);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
