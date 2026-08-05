package br.gov.ponto.satisfacao.api;

import br.gov.ponto.satisfacao.SatisfacaoService;
import br.gov.ponto.satisfacao.SentimentoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Resumo da pesquisa de satisfação (visão do RH/prefeito). */
@RestController
@RequestMapping("/api/satisfacao")
public class SatisfacaoController {

    private final SatisfacaoService satisfacaoService;
    private final SentimentoService sentimentoService;

    public SatisfacaoController(SatisfacaoService satisfacaoService, SentimentoService sentimentoService) {
        this.satisfacaoService = satisfacaoService;
        this.sentimentoService = sentimentoService;
    }

    @GetMapping("/resumo")
    public ResumoSatisfacaoResponse resumo() {
        return satisfacaoService.resumo();
    }

    /**
     * Análise de sentimento dos comentários por IA (funcionalidade IA_SENTIMENTO, ligável no painel).
     * {@code disponivel=false} = a prefeitura mantém a função desligada ou não há provedor.
     */
    @GetMapping("/sentimento")
    public SentimentoResponse sentimento() {
        return sentimentoService.analisar();
    }
}
