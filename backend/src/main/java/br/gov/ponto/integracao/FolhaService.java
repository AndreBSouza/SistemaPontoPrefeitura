package br.gov.ponto.integracao;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.relatorios.RelatorioService;
import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
import br.gov.ponto.sobreaviso.SobreavisoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

/** Exporta insumos de frequencia (por vinculo) para o sistema de folha do ente. */
@Service
public class FolhaService {

    private final VinculoRepository vinculoRepository;
    private final RelatorioService relatorioService;
    private final SobreavisoService sobreavisoService;

    public FolhaService(VinculoRepository vinculoRepository, RelatorioService relatorioService,
                        SobreavisoService sobreavisoService) {
        this.vinculoRepository = vinculoRepository;
        this.relatorioService = relatorioService;
        this.sobreavisoService = sobreavisoService;
    }

    @Transactional(readOnly = true)
    public String exportarCsv(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        StringBuilder sb = new StringBuilder(
                "matricula;vinculoId;trabalhadosMin;esperadosMin;atrasos;faltas;horaExtraMin;sobreavisoMin\n");
        for (Vinculo v : vinculoRepository.findByTenantId(tenantId)) {
            RelatorioFrequenciaResponse r = relatorioService.frequenciaMensal(v.getId(), competencia);
            sb.append(v.getMatricula()).append(';')
                    .append(v.getId()).append(';')
                    .append(r.totalMinutosTrabalhados()).append(';')
                    .append(r.totalMinutosEsperados()).append(';')
                    .append(r.qtdAtrasos()).append(';')
                    .append(r.qtdFaltas()).append(';')
                    .append(r.minutosHoraExtra()).append(';')
                    .append(sobreavisoService.totalMinutos(v.getId(), competencia)).append('\n');
        }
        return sb.toString();
    }
}
