package br.gov.ponto.saas.api;

import br.gov.ponto.cadastro.api.CriarServidorRequest;
import br.gov.ponto.saas.OnboardingServidorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Onboarding guiado do servidor (12.6.5): cadastra o servidor e já devolve o código de
 * ativação do app. Sob o caminho /api/servidores/** (RH/controladoria/tenant-admin).
 */
@RestController
@RequestMapping("/api/servidores/onboarding")
public class OnboardingServidorController {

    private final OnboardingServidorService onboardingServidorService;

    public OnboardingServidorController(OnboardingServidorService onboardingServidorService) {
        this.onboardingServidorService = onboardingServidorService;
    }

    @PostMapping
    public ResponseEntity<OnboardingServidorResponse> onboard(@Valid @RequestBody CriarServidorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onboardingServidorService.onboard(request));
    }
}
