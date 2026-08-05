package br.gov.ponto.tenant.api;

import br.gov.ponto.tenant.LogoService;
import br.gov.ponto.tenant.domain.TenantLogo;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serve o logo do ente pelo slug — público (login/app exibem antes de autenticar). */
@RestController
@RequestMapping("/api/publico/branding")
public class LogoPublicoController {

    private final LogoService logoService;

    public LogoPublicoController(LogoService logoService) {
        this.logoService = logoService;
    }

    @GetMapping("/{slug}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable String slug) {
        TenantLogo logo = logoService.buscarPorSlug(slug);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.getContentType()))
                .body(logo.getConteudo());
    }
}
