package br.gov.ponto.integracao;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Regime;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.integracao.api.EsocialJornadaResponse;
import br.gov.ponto.integracao.api.EventoJornada;
import br.gov.ponto.relatorios.RelatorioService;
import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Disponibiliza dados de jornada para o eSocial (vinculos celetistas).
 * Conteudo representativo — validar contra o leiaute vigente antes da transmissao.
 */
@Service
public class EsocialService {

    private final VinculoRepository vinculoRepository;
    private final RelatorioService relatorioService;

    public EsocialService(VinculoRepository vinculoRepository, RelatorioService relatorioService) {
        this.vinculoRepository = vinculoRepository;
        this.relatorioService = relatorioService;
    }

    @Transactional(readOnly = true)
    public EsocialJornadaResponse gerarEventosJornada(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        List<EventoJornada> eventos = new ArrayList<>();
        for (Vinculo v : vinculoRepository.findByTenantId(tenantId)) {
            if (v.getRegime() == Regime.CELETISTA) {
                RelatorioFrequenciaResponse r = relatorioService.frequenciaMensal(v.getId(), competencia);
                eventos.add(new EventoJornada(v.getMatricula(), v.getRegime().name(),
                        r.totalMinutosTrabalhados()));
            }
        }
        return new EsocialJornadaResponse(competencia.toString(), eventos);
    }
}
