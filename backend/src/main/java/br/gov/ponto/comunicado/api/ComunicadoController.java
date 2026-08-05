package br.gov.ponto.comunicado.api;

import br.gov.ponto.comunicado.ComunicadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Publicação e listagem de comunicados oficiais (painel da prefeitura). */
@RestController
@RequestMapping("/api/comunicados")
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    public ComunicadoController(ComunicadoService comunicadoService) {
        this.comunicadoService = comunicadoService;
    }

    @PostMapping
    public ResponseEntity<ComunicadoResponse> publicar(@Valid @RequestBody PublicarComunicadoRequest request) {
        var c = comunicadoService.publicar(request.titulo(), request.mensagem(), request.lotacaoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ComunicadoResponse.from(c));
    }

    @GetMapping
    public List<ComunicadoResponse> listar() {
        return comunicadoService.listarTodos().stream().map(ComunicadoResponse::from).toList();
    }
}
