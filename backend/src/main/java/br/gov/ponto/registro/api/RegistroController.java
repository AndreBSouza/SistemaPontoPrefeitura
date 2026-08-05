package br.gov.ponto.registro.api;

import br.gov.ponto.registro.RegistroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/registros")
public class RegistroController {

    private final RegistroService registroService;

    public RegistroController(RegistroService registroService) {
        this.registroService = registroService;
    }

    @PostMapping
    public ResponseEntity<ComprovanteResponse> registrar(@Valid @RequestBody RegistrarPontoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroService.registrar(request));
    }

    /** Botao unico: o servidor deduz o tipo da batida (entrada/intervalo/saida). */
    @PostMapping("/bater")
    public ResponseEntity<BatidaResponse> bater(@Valid @RequestBody BaterPontoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroService.bater(request));
    }

    @GetMapping
    public List<ComprovanteResponse> listarPorVinculo(@RequestParam UUID vinculoId) {
        return registroService.listarPorVinculo(vinculoId);
    }
}
