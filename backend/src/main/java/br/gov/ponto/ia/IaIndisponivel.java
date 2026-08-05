package br.gov.ponto.ia;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementação default: NENHUM provedor de IA configurado. {@link #disponivel()} retorna
 * {@code false}, então os serviços de IA respondem "indisponível" de forma graciosa. Os métodos
 * de execução lançam exceção porque só seriam chamados se {@code disponivel()} fosse true.
 */
@Service
public class IaIndisponivel implements IaProvider {

    @Override
    public boolean disponivel() {
        return false;
    }

    @Override
    public String responderAssistente(String pergunta, UUID vinculoId) {
        throw new IaIndisponivelException();
    }

    @Override
    public OcrAtestado lerAtestado(byte[] imagem, String contentType) {
        throw new IaIndisponivelException();
    }

    @Override
    public String resumirGestao(String contexto) {
        throw new IaIndisponivelException();
    }

    @Override
    public SentimentoIa analisarSentimento(List<String> comentarios) {
        throw new IaIndisponivelException();
    }
}
