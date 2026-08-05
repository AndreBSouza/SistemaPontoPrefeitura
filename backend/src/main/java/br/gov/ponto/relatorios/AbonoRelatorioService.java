package br.gov.ponto.relatorios;

import br.gov.ponto.apuracao.JustificativaRepository;
import br.gov.ponto.apuracao.domain.Justificativa;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.relatorios.api.AbonoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Relatório de abonos e exceções (12.6.15) com exportação (12.6.6): justificativas que
 * tocam a competência, com a decisão (motivo/quando/quem). "Quem aprovou" fica disponível
 * quando o login administrativo (sub do JWT) estiver mapeado ao aprovadorId.
 */
@Service
public class AbonoRelatorioService {

    private final JustificativaRepository justificativaRepository;
    private final ServidorRepository servidorRepository;
    private final VinculoRepository vinculoRepository;

    public AbonoRelatorioService(JustificativaRepository justificativaRepository,
                                 ServidorRepository servidorRepository, VinculoRepository vinculoRepository) {
        this.justificativaRepository = justificativaRepository;
        this.servidorRepository = servidorRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional(readOnly = true)
    public List<AbonoResponse> abonos(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Map<UUID, String> nomes = servidorRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Servidor::getId, Servidor::getNome, (a, b) -> a));
        Map<UUID, UUID> servidorDoVinculo = vinculoRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Vinculo::getId, Vinculo::getServidorId, (a, b) -> a));

        return justificativaRepository
                .findByTenantIdAndDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
                        tenantId, competencia.atEndOfMonth(), competencia.atDay(1)).stream()
                .map(j -> new AbonoResponse(j.getId(), j.getVinculoId(),
                        nomes.getOrDefault(servidorDoVinculo.get(j.getVinculoId()), "?"),
                        j.getTipo(), j.getDataInicio(), j.getDataFim(), j.getStatus(),
                        j.getMotivoDecisao(), j.getDecisaoEm(), j.getAprovadorId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportarCsv(YearMonth competencia) {
        StringBuilder sb = new StringBuilder("servidor;tipo;dataInicio;dataFim;status;motivoDecisao\n");
        for (AbonoResponse a : abonos(competencia)) {
            sb.append(String.join(";",
                            csv(a.servidor()), a.tipo().name(),
                            String.valueOf(a.dataInicio()), String.valueOf(a.dataFim()),
                            a.status().name(), csv(a.motivoDecisao())))
                    .append("\n");
        }
        return sb.toString();
    }

    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        // Neutraliza separador e quebras de linha (CR e LF) para não romper a linha/coluna no CSV.
        return v.replace(';', ',').replace('\r', ' ').replace('\n', ' ');
    }
}
