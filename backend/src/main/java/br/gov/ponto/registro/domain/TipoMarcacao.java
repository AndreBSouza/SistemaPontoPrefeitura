package br.gov.ponto.registro.domain;

/** Tipo de marcacao de ponto, com rotulo amigavel para o servidor. */
public enum TipoMarcacao {
    ENTRADA("Entrada"),
    SAIDA("Saída"),
    INTERVALO_INICIO("Início do intervalo"),
    INTERVALO_FIM("Fim do intervalo");

    private final String rotulo;

    TipoMarcacao(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Rotulo legivel (ex.: "Entrada", "Início do intervalo"). */
    public String rotulo() {
        return rotulo;
    }
}
