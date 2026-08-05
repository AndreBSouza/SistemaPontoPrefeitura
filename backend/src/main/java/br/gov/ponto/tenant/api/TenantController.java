package br.gov.ponto.tenant.api;

import br.gov.ponto.tenant.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> criar(@Valid @RequestBody CriarTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.criar(request));
    }

    @GetMapping
    public List<TenantResponse> listar() {
        return tenantService.listar();
    }
}
