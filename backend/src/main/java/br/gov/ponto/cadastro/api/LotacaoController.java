package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.LotacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lotacoes")
public class LotacaoController {

    private final LotacaoService lotacaoService;

    public LotacaoController(LotacaoService lotacaoService) {
        this.lotacaoService = lotacaoService;
    }

    @PostMapping
    public ResponseEntity<LotacaoResponse> criar(@Valid @RequestBody CriarLotacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LotacaoResponse.from(lotacaoService.criar(request.nome(), request.sigla())));
    }

    @GetMapping
    public List<LotacaoResponse> listar() {
        return lotacaoService.listar().stream().map(LotacaoResponse::from).toList();
    }

    @PutMapping("/{id}/chefia")
    public ResponseEntity<Void> definirChefia(@PathVariable UUID id, @RequestParam UUID servidorId) {
        lotacaoService.definirChefia(id, servidorId);
        return ResponseEntity.noContent().build();
    }

    /** Define as regras de ponto proprias do orgao (jornada padrao, tolerancia, banco de horas, geofence). */
    @PutMapping("/{id}/regras")
    public LotacaoResponse definirRegras(@PathVariable UUID id, @Valid @RequestBody DefinirRegrasRequest request) {
        return LotacaoResponse.from(lotacaoService.definirRegras(id, request.paraDominio()));
    }
}
