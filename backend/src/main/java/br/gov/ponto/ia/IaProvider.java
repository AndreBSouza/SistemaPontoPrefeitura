package br.gov.ponto.ia;

import java.util.List;
import java.util.UUID;

/**
 * Ponto de plugue (seam) para IA. A implementação default ({@link IaIndisponivel}) não faz nada;
 * em produção, fornecer um bean {@code @Primary} que chame um provedor real (LLM/OCR). Como o
 * plugue já existe, basta adicionar a implementação — o resto do sistema não muda. Cada recurso
 * ainda é ligado por ente no painel (funcionalidades IA_ASSISTENTE / IA_OCR / IA_RESUMO).
 */
public interface IaProvider {

    /** {@code false} quando não há provedor configurado (default). */
    boolean disponivel();

    /** Assistente/chatbot do servidor (ex.: "quanto de banco de horas eu tenho?"). */
    String responderAssistente(String pergunta, UUID vinculoId);

    /** OCR de atestado médico: extrai CID, início e dias para pré-preencher a justificativa. */
    OcrAtestado lerAtestado(byte[] imagem, String contentType);

    /** Resumo executivo em linguagem natural a partir do contexto de indicadores do mês. */
    String resumirGestao(String contexto);

    /** Classifica o sentimento de uma lista de comentários (contagens positivos/neutros/negativos + resumo). */
    SentimentoIa analisarSentimento(List<String> comentarios);
}
