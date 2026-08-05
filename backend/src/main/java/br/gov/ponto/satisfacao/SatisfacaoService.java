package br.gov.ponto.satisfacao;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.satisfacao.api.ResumoSatisfacaoResponse;
import br.gov.ponto.satisfacao.domain.PesquisaSatisfacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pesquisa de satisfação/feedback (12.1.3): o servidor avalia (1..5) e o RH/prefeito vê a
 * aceitação agregada (total, média, distribuição) — métrica de adoção.
 */
@Service
public class SatisfacaoService {

    private final SatisfacaoRepository satisfacaoRepository;

    public SatisfacaoService(SatisfacaoRepository satisfacaoRepository) {
        this.satisfacaoRepository = satisfacaoRepository;
    }

    @Transactional
    public PesquisaSatisfacao registrar(UUID vinculoId, int nota, String comentario) {
        if (nota < 1 || nota > 5) {
            throw new IllegalArgumentException("Nota deve estar entre 1 e 5");
        }
        return satisfacaoRepository.save(
                new PesquisaSatisfacao(TenantContext.requireCurrent(), vinculoId, nota, comentario));
    }

    @Transactional(readOnly = true)
    public ResumoSatisfacaoResponse resumo() {
        List<PesquisaSatisfacao> todas = satisfacaoRepository.findByTenantId(TenantContext.requireCurrent());
        Map<Integer, Integer> distribuicao = new LinkedHashMap<>();
        for (int n = 1; n <= 5; n++) {
            distribuicao.put(n, 0);
        }
        int soma = 0;
        List<String> comentarios = new java.util.ArrayList<>();
        for (PesquisaSatisfacao p : todas) {
            distribuicao.merge(p.getNota(), 1, Integer::sum);
            soma += p.getNota();
            if (p.getComentario() != null && !p.getComentario().isBlank()) {
                comentarios.add(p.getComentario());
            }
        }
        double media = todas.isEmpty() ? 0.0 : Math.round((double) soma / todas.size() * 100.0) / 100.0;
        return new ResumoSatisfacaoResponse(todas.size(), media, distribuicao, comentarios);
    }
}
