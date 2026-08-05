package br.gov.ponto.delegacao.api;

import br.gov.ponto.delegacao.DelegacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Delegação de aprovação (substituto do gestor nas férias). */
@RestController
@RequestMapping("/api/delegacoes")
public class DelegacaoController {

    private final DelegacaoService delegacaoService;

    public DelegacaoController(DelegacaoService delegacaoService) {
        this.delegacaoService = delegacaoService;
    }

    @PostMapping
    public ResponseEntity<DelegacaoResponse> criar(@Valid @RequestBody CriarDelegacaoRequest request) {
        var d = delegacaoService.criar(request.deleganteServidorId(), request.delegadoServidorId(),
                request.dataInicio(), request.dataFim());
        return ResponseEntity.status(HttpStatus.CREATED).body(DelegacaoResponse.from(d));
    }

    @GetMapping
    public List<DelegacaoResponse> listar() {
        return delegacaoService.listar().stream().map(DelegacaoResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revogar(@PathVariable UUID id) {
        delegacaoService.revogar(id);
        return ResponseEntity.noContent().build();
    }
}
