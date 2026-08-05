package br.gov.ponto.tenant.api;

import br.gov.ponto.tenant.BrandingService;
import br.gov.ponto.tenant.LogoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Identidade visual do ente (white-label), definida pelo RH/admin do município. */
@RestController
@RequestMapping("/api/branding")
public class BrandingController {

    private final BrandingService brandingService;
    private final LogoService logoService;

    public BrandingController(BrandingService brandingService, LogoService logoService) {
        this.brandingService = brandingService;
        this.logoService = logoService;
    }

    @GetMapping
    public BrandingResponse obter() {
        return brandingService.obter();
    }

    @PutMapping
    public BrandingResponse definir(@Valid @RequestBody DefinirBrandingRequest request) {
        return brandingService.definir(request);
    }

    /**
     * Envia o logo do ente (bytes da imagem no corpo; Content-Type indica o formato). Armazenado
     * no banco (12.2.1, alternativa ao S3) e exposto via {@code Branding.logoUrl} público.
     */
    @PostMapping("/logo")
    public ResponseEntity<BrandingResponse> enviarLogo(@RequestBody byte[] conteudo,
                                                       @RequestHeader("Content-Type") String contentType) {
        logoService.salvar(conteudo, contentType);
        return ResponseEntity.ok(brandingService.obter());
    }

    /** Define o subdomínio do ente (autoconfiguração do app/login, 12.2.5). */
    @PutMapping("/subdominio")
    public ResponseEntity<Void> definirSubdominio(@RequestParam String valor) {
        brandingService.definirSubdominio(valor);
        return ResponseEntity.noContent().build();
    }

    /** Define o CNPJ do ente (14 dígitos) — obrigatório para emitir o AFD (Portaria 671). */
    @PutMapping("/cnpj")
    public ResponseEntity<Void> definirCnpj(@RequestParam String valor) {
        brandingService.definirCnpj(valor);
        return ResponseEntity.noContent().build();
    }
}
