package br.gov.ponto.publico.api;

import br.gov.ponto.publico.TransparenciaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Portal público de transparência ativa (dados abertos agregados, sem autenticação). */
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final TransparenciaService transparenciaService;

    public PublicoController(TransparenciaService transparenciaService) {
        this.transparenciaService = transparenciaService;
    }

    @GetMapping("/transparencia/{slug}")
    public TransparenciaResponse transparencia(
            @PathVariable String slug,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return transparenciaService.publico(slug, competencia);
    }
}
