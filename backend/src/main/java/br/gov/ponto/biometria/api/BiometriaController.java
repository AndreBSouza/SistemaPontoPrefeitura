package br.gov.ponto.biometria.api;

import br.gov.ponto.biometria.BiometriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/biometria")
public class BiometriaController {

    private final BiometriaService biometriaService;

    public BiometriaController(BiometriaService biometriaService) {
        this.biometriaService = biometriaService;
    }

    @PostMapping("/referencia")
    public ResponseEntity<Void> cadastrarReferencia(@Valid @RequestBody CadastrarReferenciaRequest request) {
        biometriaService.cadastrarReferencia(request.servidorId(), request.referencia());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
