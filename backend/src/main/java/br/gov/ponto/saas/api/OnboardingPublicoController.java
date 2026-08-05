package br.gov.ponto.saas.api;

import br.gov.ponto.saas.LimiteOnboarding;
import br.gov.ponto.saas.SolicitacaoEnteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoint PÚBLICO de adesão self-service do ente (12.3.13). Pré-autenticado e com limite de taxa;
 * apenas registra a solicitação (PENDENTE) — não cria o tenant, que depende de aprovação do operador.
 */
@RestController
@RequestMapping("/api/publico/onboarding")
public class OnboardingPublicoController {

    private final SolicitacaoEnteService solicitacaoEnteService;
    private final LimiteOnboarding limiteOnboarding;

    public OnboardingPublicoController(SolicitacaoEnteService solicitacaoEnteService,
                                       LimiteOnboarding limiteOnboarding) {
        this.solicitacaoEnteService = solicitacaoEnteService;
        this.limiteOnboarding = limiteOnboarding;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoEnteResponse> solicitar(@Valid @RequestBody SolicitarEnteRequest request,
                                                             HttpServletRequest http) {
        if (!limiteOnboarding.permitir(http.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas solicitações deste endereço. Tente novamente mais tarde.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitacaoEnteService.solicitar(request));
    }
}
