package br.gov.ponto.registro.api;

import br.gov.ponto.registro.TotemService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Totem de ponto: registro por matrícula (servidor sem smartphone). */
@RestController
@RequestMapping("/api/totem")
public class TotemController {

    private final TotemService totemService;

    public TotemController(TotemService totemService) {
        this.totemService = totemService;
    }

    @PostMapping("/bater")
    public ResponseEntity<BatidaResponse> bater(@RequestParam @NotBlank String matricula) {
        return ResponseEntity.status(HttpStatus.CREATED).body(totemService.baterPorMatricula(matricula));
    }
}
