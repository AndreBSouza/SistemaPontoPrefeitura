package br.gov.ponto.ia;

import br.gov.ponto.funcionalidade.FuncionalidadeService;
import br.gov.ponto.funcionalidade.domain.Funcionalidade;
import br.gov.ponto.ia.api.ResumoIaResponse;
import br.gov.ponto.relatorios.IndicadoresService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/** Resumo executivo por IA a partir dos indicadores do mês (IA_RESUMO), só se houver provedor. */
@Service
public class ResumoIaService {

    private final FuncionalidadeService funcionalidadeService;
    private final IndicadoresService indicadoresService;
    private final IaProvider ia;

    public ResumoIaService(FuncionalidadeService funcionalidadeService,
                           IndicadoresService indicadoresService, IaProvider ia) {
        this.funcionalidadeService = funcionalidadeService;
        this.indicadoresService = indicadoresService;
        this.ia = ia;
    }

    @Transactional(readOnly = true)
    public ResumoIaResponse resumir(YearMonth competencia) {
        if (!funcionalidadeService.habilitada(Funcionalidade.IA_RESUMO) || !ia.disponivel()) {
            return ResumoIaResponse.indisponivel();
        }
        String contexto = "Indicadores de " + competencia + ": " + indicadoresService.obter(competencia);
        return new ResumoIaResponse(true, ia.resumirGestao(contexto));
    }
}
