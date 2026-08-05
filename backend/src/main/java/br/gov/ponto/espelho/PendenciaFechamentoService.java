package br.gov.ponto.espelho;

import br.gov.ponto.cadastro.LotacaoRepository;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.api.PendenciaFechamentoResponse;
import br.gov.ponto.espelho.domain.Competencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-model do painel "o que falta fechar" (12.6.2): cruza os vínculos ativos com o
 * status da competência e agrupa as pendências por órgão para o acompanhamento do RH.
 */
@Service
public class PendenciaFechamentoService {

    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final LotacaoRepository lotacaoRepository;
    private final CompetenciaRepository competenciaRepository;

    public PendenciaFechamentoService(VinculoRepository vinculoRepository, ServidorRepository servidorRepository,
                                      LotacaoRepository lotacaoRepository, CompetenciaRepository competenciaRepository) {
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.competenciaRepository = competenciaRepository;
    }

    @Transactional(readOnly = true)
    public PendenciaFechamentoResponse pendentes(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        LocalDate anoMes = competencia.atDay(1);

        List<Vinculo> ativos = vinculoRepository.findByTenantId(tenantId).stream()
                .filter(Vinculo::isAtivo).toList();
        Map<UUID, String> nomes = servidorRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Servidor::getId, Servidor::getNome, (a, b) -> a));
        Map<UUID, String> orgaos = lotacaoRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Lotacao::getId, Lotacao::getNome, (a, b) -> a));

        int fechadas = 0;
        Map<UUID, List<PendenciaFechamentoResponse.Item>> porOrgao = new LinkedHashMap<>();
        for (Vinculo v : ativos) {
            boolean fechada = competenciaRepository
                    .findByVinculoIdAndTenantIdAndAnoMes(v.getId(), tenantId, anoMes)
                    .map(Competencia::isFechada).orElse(false);
            if (fechada) {
                fechadas++;
                continue;
            }
            porOrgao.computeIfAbsent(v.getLotacaoId(), k -> new ArrayList<>())
                    .add(new PendenciaFechamentoResponse.Item(v.getId(), v.getMatricula(),
                            v.getServidorId(), nomes.getOrDefault(v.getServidorId(), "?")));
        }

        List<PendenciaFechamentoResponse.OrgaoPendencia> grupos = porOrgao.entrySet().stream()
                .map(e -> new PendenciaFechamentoResponse.OrgaoPendencia(
                        e.getKey(),
                        e.getKey() == null ? "Sem órgão" : orgaos.getOrDefault(e.getKey(), "?"),
                        e.getValue().size(), e.getValue()))
                .toList();

        return new PendenciaFechamentoResponse(competencia.toString(), ativos.size(),
                fechadas, ativos.size() - fechadas, grupos);
    }
}
