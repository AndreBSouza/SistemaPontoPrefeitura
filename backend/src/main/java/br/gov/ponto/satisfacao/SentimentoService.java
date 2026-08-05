package br.gov.ponto.satisfacao;

import br.gov.ponto.funcionalidade.FuncionalidadeService;
import br.gov.ponto.funcionalidade.domain.Funcionalidade;
import br.gov.ponto.ia.IaProvider;
import br.gov.ponto.ia.SentimentoIa;
import br.gov.ponto.satisfacao.api.SentimentoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Análise de sentimento dos comentários da pesquisa (IA_SENTIMENTO), só se houver provedor. */
@Service
public class SentimentoService {

    /** Teto de comentários enviados ao provedor por análise (custo/tamanho previsíveis). */
    private static final int MAX_COMENTARIOS = 300;

    private final FuncionalidadeService funcionalidadeService;
    private final SatisfacaoService satisfacaoService;
    private final IaProvider ia;

    public SentimentoService(FuncionalidadeService funcionalidadeService,
                             SatisfacaoService satisfacaoService, IaProvider ia) {
        this.funcionalidadeService = funcionalidadeService;
        this.satisfacaoService = satisfacaoService;
        this.ia = ia;
    }

    @Transactional(readOnly = true)
    public SentimentoResponse analisar() {
        if (!funcionalidadeService.habilitada(Funcionalidade.IA_SENTIMENTO) || !ia.disponivel()) {
            return SentimentoResponse.indisponivel();
        }
        List<String> comentarios = satisfacaoService.resumo().comentarios();
        if (comentarios.isEmpty()) {
            return new SentimentoResponse(true, 0, 0, 0, 0, "Sem comentários para analisar no período.");
        }
        List<String> amostra = comentarios.size() > MAX_COMENTARIOS
                ? comentarios.subList(0, MAX_COMENTARIOS) : comentarios;
        SentimentoIa s = ia.analisarSentimento(amostra);
        // Total derivado da SOMA das categorias (internamente consistente, mesmo se a IA classificar
        // a menos): evita "total" divergente das contagens.
        int total = s.positivos() + s.neutros() + s.negativos();
        return new SentimentoResponse(true, total, s.positivos(), s.neutros(), s.negativos(), s.resumo());
    }
}
