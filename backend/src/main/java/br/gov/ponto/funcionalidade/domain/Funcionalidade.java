package br.gov.ponto.funcionalidade.domain;

/** Funcionalidades que o ente pode ligar/desligar no painel. Todas opt-in (padrão desligado). */
public enum Funcionalidade {

    ANOMALIAS("Detecção de anomalias (heurística, sem IA)"),
    IA_ASSISTENTE("Assistente virtual do servidor (IA)"),
    IA_OCR("OCR de atestados (IA)"),
    IA_RESUMO("Resumo inteligente para o gestor (IA)"),
    IA_SENTIMENTO("Análise de sentimento da pesquisa (IA)");

    private final String rotulo;

    Funcionalidade(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Padrão do sistema: todas desligadas até o ente ativar. */
    public boolean padrao() {
        return false;
    }
}
