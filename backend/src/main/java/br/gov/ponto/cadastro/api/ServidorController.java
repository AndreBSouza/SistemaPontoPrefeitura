package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.ImportacaoServidorService;
import br.gov.ponto.cadastro.ServidorService;
import br.gov.ponto.cadastro.api.ImportacaoResultado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/servidores")
public class ServidorController {

    private final ServidorService servidorService;
    private final ImportacaoServidorService importacaoServidorService;

    public ServidorController(ServidorService servidorService,
                             ImportacaoServidorService importacaoServidorService) {
        this.servidorService = servidorService;
        this.importacaoServidorService = importacaoServidorService;
    }

    @PostMapping
    public ResponseEntity<ServidorResponse> criar(@Valid @RequestBody CriarServidorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servidorService.criar(request));
    }

    @GetMapping
    public List<ServidorResponse> listar() {
        return servidorService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServidorResponse> buscar(@PathVariable UUID id) {
        return servidorService.buscar(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/importar", consumes = {"text/csv", "text/plain"})
    public ImportacaoResultado importar(
            @RequestBody String csv,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UUID lotacaoId) {
        return importacaoServidorService.importarCsv(csv, lotacaoId);
    }

    @org.springframework.web.bind.annotation.PutMapping("/vinculos/{vinculoId}/lotacao")
    public ResponseEntity<Void> lotarVinculo(@PathVariable UUID vinculoId,
                                             @org.springframework.web.bind.annotation.RequestParam UUID lotacaoId) {
        servidorService.lotarVinculo(vinculoId, lotacaoId);
        return ResponseEntity.noContent().build();
    }
}
