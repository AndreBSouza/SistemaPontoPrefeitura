package br.gov.ponto.integracao.api;

import br.gov.ponto.integracao.EsocialService;
import br.gov.ponto.integracao.FolhaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/integracoes")
public class IntegracaoController {

    private final FolhaService folhaService;
    private final EsocialService esocialService;

    public IntegracaoController(FolhaService folhaService, EsocialService esocialService) {
        this.folhaService = folhaService;
        this.esocialService = esocialService;
    }

    @GetMapping(value = "/folha/csv", produces = "text/csv")
    public ResponseEntity<String> folhaCsv(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(folhaService.exportarCsv(competencia));
    }

    @GetMapping("/esocial/jornada")
    public EsocialJornadaResponse esocialJornada(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return esocialService.gerarEventosJornada(competencia);
    }
}
