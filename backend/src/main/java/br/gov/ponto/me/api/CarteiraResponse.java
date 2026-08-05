package br.gov.ponto.me.api;

/**
 * Carteira funcional digital (12.3.8): identifica o servidor no app com os dados do vínculo
 * e a identidade visual do ente (white-label).
 */
public record CarteiraResponse(
        String nome,
        String cpf,
        String matricula,
        String cargo,
        String regime,
        String orgao,
        String ente,
        String logoUrl,
        String corPrimaria
) {
}
