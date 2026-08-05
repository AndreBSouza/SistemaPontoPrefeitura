package br.gov.ponto.saas.domain;

/** Forma de contratação do sistema pelo ente público (Lei 14.133/2021). */
public enum ModalidadeContratacao {

    DISPENSA("Dispensa de licitação"),
    PREGAO("Pregão eletrônico"),
    INEXIGIBILIDADE("Inexigibilidade"),
    CONCORRENCIA("Concorrência"),
    ADESAO_ATA("Adesão a ata de registro de preços"),
    OUTRA("Outra");

    private final String rotulo;

    ModalidadeContratacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
