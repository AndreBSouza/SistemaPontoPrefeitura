package br.gov.ponto.saas;

import br.gov.ponto.ativacao.AtivacaoService;
import br.gov.ponto.ativacao.api.GerarCodigoResponse;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.cadastro.api.ServidorResponse;
import br.gov.ponto.saas.api.OnboardingServidorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Onboarding guiado do servidor (12.6.5): num passo só, cadastra o servidor (com vínculo) e
 * já gera o código de ativação do app — fim do vai-e-volta cadastro → gerar código.
 */
@Service
public class OnboardingServidorService {

    private final ServidorService servidorService;
    private final AtivacaoService ativacaoService;

    public OnboardingServidorService(ServidorService servidorService, AtivacaoService ativacaoService) {
        this.servidorService = servidorService;
        this.ativacaoService = ativacaoService;
    }

    @Transactional
    public OnboardingServidorResponse onboard(CriarServidorRequest request) {
        ServidorResponse servidor = servidorService.criar(request);
        GerarCodigoResponse codigo = null;
        if (!servidor.vinculos().isEmpty()) {
            UUID primeiroVinculo = servidor.vinculos().get(0).id();
            codigo = ativacaoService.gerar(primeiroVinculo, null);
        }
        return new OnboardingServidorResponse(servidor, codigo);
    }
}
