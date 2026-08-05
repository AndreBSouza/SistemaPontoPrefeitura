package br.gov.ponto.integracao;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.integracao.api.ConferenciaRequest;
import br.gov.ponto.integracao.api.ConferenciaResponse;
import br.gov.ponto.relatorios.RelatorioService;
import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Conferência cruzada folha × frequência (12.6.12): para cada vínculo, compara a hora extra
 * paga e as faltas descontadas (informadas pela folha) com o que a frequência apurou,
 * apontando divergências para o RH conciliar antes de pagar.
 */
@Service
public class ConferenciaFolhaService {

    private final RelatorioService relatorioService;

    public ConferenciaFolhaService(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @Transactional(readOnly = true)
    public ConferenciaResponse conferir(YearMonth competencia, ConferenciaRequest request) {
        TenantContext.requireCurrent();
        List<ConferenciaResponse.Item> itens = new ArrayList<>();
        int divergencias = 0;
        for (ConferenciaRequest.ItemFolha folha : request.itens()) {
            RelatorioFrequenciaResponse freq = relatorioService.frequenciaMensal(folha.vinculoId(), competencia);
            boolean divergente = freq.minutosHoraExtra() != folha.horaExtraPagaMinutos()
                    || freq.qtdFaltas() != folha.faltasDescontadas();
            if (divergente) {
                divergencias++;
            }
            itens.add(new ConferenciaResponse.Item(folha.vinculoId(),
                    freq.minutosHoraExtra(), folha.horaExtraPagaMinutos(),
                    freq.qtdFaltas(), folha.faltasDescontadas(), divergente));
        }
        return new ConferenciaResponse(competencia.toString(), divergencias, itens);
    }
}
