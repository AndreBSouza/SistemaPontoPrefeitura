package br.gov.ponto.saas.api;

import br.gov.ponto.saas.OnboardingService;
import br.gov.ponto.saas.SolicitacaoEnteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Onboarding de ente pelo operador da plataforma + fila de solicitações self-service (12.3.13). */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final SolicitacaoEnteService solicitacaoEnteService;

    public OnboardingController(OnboardingService onboardingService,
                               SolicitacaoEnteService solicitacaoEnteService) {
        this.onboardingService = onboardingService;
        this.solicitacaoEnteService = solicitacaoEnteService;
    }

    @PostMapping
    public ResponseEntity<OnboardingResponse> provisionar(@Valid @RequestBody OnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingService.provisionar(request));
    }

    /** Solicitações de adesão pendentes (self-service) aguardando aprovação. */
    @GetMapping("/solicitacoes")
    public List<SolicitacaoEnteResponse> solicitacoesPendentes() {
        return solicitacaoEnteService.listarPendentes();
    }

    /** Aprova uma solicitação e provisiona o tenant. */
    @PostMapping("/solicitacoes/{id}/aprovar")
    public OnboardingResponse aprovarSolicitacao(@PathVariable UUID id) {
        return solicitacaoEnteService.aprovar(id);
    }

    @PostMapping("/solicitacoes/{id}/rejeitar")
    public ResponseEntity<Void> rejeitarSolicitacao(@PathVariable UUID id,
                                                    @RequestParam(required = false) String motivo) {
        solicitacaoEnteService.rejeitar(id, motivo);
        return ResponseEntity.noContent().build();
    }
}
