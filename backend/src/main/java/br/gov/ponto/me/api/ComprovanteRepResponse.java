package br.gov.ponto.me.api;

/**
 * Comprovante de Registro de Ponto do Trabalhador, com o conteúdo mínimo do <b>art. 79</b> da
 * Portaria MTP 671/2021.
 *
 * <p>Cada campo corresponde a um inciso: I título · II NSR · III empregador (nome, CNPJ/CPF e
 * CEI/CAEPF/CNO) · IV local da prestação do serviço · V trabalhador (nome e CPF) · VI data e hora
 * do registro · VII número de registro no INPI (por ser REP-P) · VIII <b>código hash SHA-256</b>
 * da marcação · IX assinatura eletrônica (exigida no comprovante impresso).</p>
 */
public record ComprovanteRepResponse(
        String titulo,
        long nsr,
        String empregadorNome,
        String empregadorCnpj,
        String empregadorCnoCaepf,
        String localPrestacaoServico,
        String trabalhadorNome,
        String trabalhadorCpf,
        String dataHoraRegistro,
        String registroInpi,
        String codigoHash,
        String assinatura
) {
    public static final String TITULO = "Comprovante de Registro de Ponto do Trabalhador";
}
