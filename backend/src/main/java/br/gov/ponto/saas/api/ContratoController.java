package br.gov.ponto.saas.api;

import br.gov.ponto.saas.ContratoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Contrato de fornecimento ao ente (operador do SaaS + admin da prefeitura). */
@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @GetMapping
    public List<ContratoResponse> listar() {
        return contratoService.listar();
    }

    @PostMapping
    public ResponseEntity<ContratoResponse> criar(@Valid @RequestBody ContratoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contratoService.criar(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        contratoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    /** Relatório de execução mensal do contrato (para o processo de pagamento). */
    @GetMapping("/execucao")
    public ExecucaoContratoResponse execucao(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return contratoService.execucao(competencia);
    }
}
