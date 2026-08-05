package br.gov.ponto.sobreaviso.api;

import br.gov.ponto.sobreaviso.SobreavisoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Sobreaviso (on-call) por vínculo — registro do RH/chefia (12.4.3). */
@RestController
@RequestMapping("/api/sobreaviso")
public class SobreavisoController {

    private final SobreavisoService sobreavisoService;

    public SobreavisoController(SobreavisoService sobreavisoService) {
        this.sobreavisoService = sobreavisoService;
    }

    @GetMapping
    public List<SobreavisoResponse> listar(@RequestParam UUID vinculoId) {
        return sobreavisoService.listarPorVinculo(vinculoId);
    }

    @PostMapping
    public ResponseEntity<SobreavisoResponse> registrar(@Valid @RequestBody RegistrarSobreavisoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sobreavisoService.registrar(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        sobreavisoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
