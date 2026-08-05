package br.gov.ponto.projeto;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.projeto.api.ProjetoRelatorioResponse;
import br.gov.ponto.projeto.domain.ApropriacaoHoras;
import br.gov.ponto.projeto.domain.Projeto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Apropriação de horas por projeto/convênio/fonte de recurso (12.4.4): apoia a prestação de
 * contas de convênios, somando as horas que cada projeto consumiu na competência.
 */
@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final ApropriacaoRepository apropriacaoRepository;
    private final VinculoRepository vinculoRepository;

    public ProjetoService(ProjetoRepository projetoRepository, ApropriacaoRepository apropriacaoRepository,
                          VinculoRepository vinculoRepository) {
        this.projetoRepository = projetoRepository;
        this.apropriacaoRepository = apropriacaoRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional
    public Projeto criar(String nome, String fonte) {
        return projetoRepository.save(new Projeto(TenantContext.requireCurrent(), nome, fonte));
    }

    @Transactional(readOnly = true)
    public List<Projeto> listar() {
        return projetoRepository.findByTenantIdOrderByNome(TenantContext.requireCurrent());
    }

    @Transactional
    public ApropriacaoHoras apropriar(UUID vinculoId, UUID projetoId, LocalDate data,
                                      int minutos, String descricao) {
        UUID tenantId = TenantContext.requireCurrent();
        if (!vinculoRepository.existsByIdAndTenantId(vinculoId, tenantId)) {
            throw new IllegalArgumentException("Vinculo inexistente no ente");
        }
        if (projetoRepository.findByIdAndTenantId(projetoId, tenantId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Projeto inexistente");
        }
        if (minutos <= 0) {
            throw new IllegalArgumentException("minutos deve ser positivo");
        }
        return apropriacaoRepository.save(
                new ApropriacaoHoras(tenantId, vinculoId, projetoId, data, minutos, descricao));
    }

    @Transactional(readOnly = true)
    public ProjetoRelatorioResponse relatorio(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Map<UUID, Projeto> projetos = new LinkedHashMap<>();
        for (Projeto p : projetoRepository.findByTenantIdOrderByNome(tenantId)) {
            projetos.put(p.getId(), p);
        }
        Map<UUID, int[]> totais = new LinkedHashMap<>(); // projetoId -> [minutos, lancamentos]
        for (ApropriacaoHoras a : apropriacaoRepository.findByTenantIdAndDataBetween(
                tenantId, competencia.atDay(1), competencia.atEndOfMonth())) {
            int[] t = totais.computeIfAbsent(a.getProjetoId(), k -> new int[2]);
            t[0] += a.getMinutos();
            t[1]++;
        }
        List<ProjetoRelatorioResponse.Linha> linhas = new ArrayList<>();
        for (Map.Entry<UUID, int[]> e : totais.entrySet()) {
            Projeto p = projetos.get(e.getKey());
            linhas.add(new ProjetoRelatorioResponse.Linha(e.getKey(),
                    p != null ? p.getNome() : "?", p != null ? p.getFonte() : null,
                    e.getValue()[0], e.getValue()[1]));
        }
        return new ProjetoRelatorioResponse(competencia.toString(), linhas);
    }
}
