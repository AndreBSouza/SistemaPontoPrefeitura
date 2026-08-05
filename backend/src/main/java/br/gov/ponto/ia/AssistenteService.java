package br.gov.ponto.ia;

import br.gov.ponto.funcionalidade.FuncionalidadeService;
import br.gov.ponto.funcionalidade.domain.Funcionalidade;
import br.gov.ponto.ia.api.AssistenteResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Assistente/chatbot do servidor, ligável no painel (IA_ASSISTENTE) e só se houver provedor. */
@Service
public class AssistenteService {

    private final FuncionalidadeService funcionalidadeService;
    private final IaProvider ia;

    public AssistenteService(FuncionalidadeService funcionalidadeService, IaProvider ia) {
        this.funcionalidadeService = funcionalidadeService;
        this.ia = ia;
    }

    public AssistenteResponse perguntar(String pergunta, UUID vinculoId) {
        if (!funcionalidadeService.habilitada(Funcionalidade.IA_ASSISTENTE) || !ia.disponivel()) {
            return AssistenteResponse.indisponivel();
        }
        return new AssistenteResponse(true, ia.responderAssistente(pergunta, vinculoId));
    }
}
