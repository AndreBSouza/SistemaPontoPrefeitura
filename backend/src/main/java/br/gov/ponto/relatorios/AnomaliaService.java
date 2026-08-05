package br.gov.ponto.relatorios;

import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.funcionalidade.FuncionalidadeService;
import br.gov.ponto.funcionalidade.domain.Funcionalidade;
import br.gov.ponto.relatorios.api.AnomaliaResponse;
import br.gov.ponto.relatorios.api.AnomaliaResponse.Anomalia;
import br.gov.ponto.relatorios.domain.DetectorAnomalia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detecção de anomalias HEURÍSTICA (sem IA), ligável pelo painel (funcionalidade ANOMALIAS).
 * Hoje sinaliza "hora extra atípica" — servidores muito acima da média de HE do ente no mês.
 */
@Service
public class AnomaliaService {

    private static final double FATOR = 2.0;   // acima de 2× a média...
    private static final int PISO_MIN = 600;   // ...e de 10h no mês (evita falso positivo)

    private final FuncionalidadeService funcionalidadeService;
    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final RelatorioService relatorioService;

    public AnomaliaService(FuncionalidadeService funcionalidadeService,
                           VinculoRepository vinculoRepository,
                           ServidorRepository servidorRepository,
                           RelatorioService relatorioService) {
        this.funcionalidadeService = funcionalidadeService;
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.relatorioService = relatorioService;
    }

    @Transactional(readOnly = true)
    public AnomaliaResponse detectar(YearMonth competencia) {
        if (!funcionalidadeService.habilitada(Funcionalidade.ANOMALIAS)) {
            return new AnomaliaResponse(false, List.of());
        }
        UUID tenantId = TenantContext.requireCurrent();
        List<Vinculo> vinculos = vinculoRepository.findByTenantId(tenantId);

        Map<UUID, Integer> horaExtraPorVinculo = new HashMap<>();
        Map<UUID, String> nomePorVinculo = new HashMap<>();
        for (Vinculo v : vinculos) {
            int he = relatorioService.frequenciaMensal(v.getId(), competencia).minutosHoraExtra();
            horaExtraPorVinculo.put(v.getId(), he);
            nomePorVinculo.put(v.getId(), servidorRepository.findByIdAndTenantId(v.getServidorId(), tenantId)
                    .map(Servidor::getNome).orElse("Servidor"));
        }

        List<Anomalia> anomalias = new ArrayList<>();
        for (UUID vid : DetectorAnomalia.outliers(horaExtraPorVinculo, FATOR, PISO_MIN)) {
            int horas = horaExtraPorVinculo.get(vid) / 60;
            anomalias.add(new Anomalia("HORA_EXTRA_ATIPICA", vid, nomePorVinculo.get(vid),
                    "Hora extra muito acima da média do ente (" + horas + "h no mês)."));
        }
        return new AnomaliaResponse(true, anomalias);
    }
}
