package br.gov.ponto.relatorios.api;

import br.gov.ponto.relatorios.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/conformidade")
public class ConformidadeController {

    private final RelatorioService relatorioService;

    public ConformidadeController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/in008")
    public ConformidadeResponse in008(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return relatorioService.conformidadeIn008(competencia);
    }
}
