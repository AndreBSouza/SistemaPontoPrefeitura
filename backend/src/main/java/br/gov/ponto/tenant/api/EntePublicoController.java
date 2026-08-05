package br.gov.ponto.tenant.api;

import br.gov.ponto.tenant.BrandingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Resolução pública do ente pelo subdomínio — o app/login se autoconfigura (12.2.5). */
@RestController
@RequestMapping("/api/publico/ente")
public class EntePublicoController {

    private final BrandingService brandingService;

    public EntePublicoController(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @GetMapping("/{subdominio}")
    public EntePublicoResponse porSubdominio(@PathVariable String subdominio) {
        return brandingService.resolverPorSubdominio(subdominio);
    }
}
