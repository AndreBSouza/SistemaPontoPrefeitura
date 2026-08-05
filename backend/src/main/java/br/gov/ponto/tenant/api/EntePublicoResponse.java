package br.gov.ponto.tenant.api;

/**
 * Dados públicos do ente para autoconfiguração do app/login a partir do subdomínio (12.2.5).
 * Sem informação sensível — só identidade visual e o slug para autenticar em seguida.
 */
public record EntePublicoResponse(
        String slug,
        String nomeApp,
        String logoUrl,
        String corPrimaria,
        String corAcento
) {
}
