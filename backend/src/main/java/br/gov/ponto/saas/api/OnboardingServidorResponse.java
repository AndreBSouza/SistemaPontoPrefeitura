package br.gov.ponto.saas.api;

import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.api.ServidorResponse;

/** Resultado do onboarding guiado do servidor: o servidor criado + o código de ativação. */
public record OnboardingServidorResponse(
        ServidorResponse servidor,
        GerarCodigoResponse codigoAtivacao
) {
}
