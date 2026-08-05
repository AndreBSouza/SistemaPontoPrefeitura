package br.gov.ponto.jornada.api;

import br.gov.ponto.jornada.OtimizacaoEscalaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.UUID;

/** Sugestão de escala (12.4.11) — sob /api/escalas (RH/controladoria/admin). */
@RestController
@RequestMapping("/api/escalas")
public class OtimizacaoEscalaController {

    private final OtimizacaoEscalaService otimizacaoEscalaService;

    public OtimizacaoEscalaController(OtimizacaoEscalaService otimizacaoEscalaService) {
        this.otimizacaoEscalaService = otimizacaoEscalaService;
    }

    /**
     * Distribui a cobertura diária do órgão de forma justa, respeitando ausências programadas e
     * um teto de dias consecutivos. É apenas sugestão — o gestor decide se aplica.
     */
    @GetMapping("/sugestao")
    public SugestaoEscalaResponse sugestao(
            @RequestParam UUID lotacaoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
            @RequestParam(defaultValue = "1") int cobertura,
            @RequestParam(defaultValue = "5") int maxConsecutivos) {
        return otimizacaoEscalaService.sugerir(lotacaoId, competencia, cobertura, maxConsecutivos);
    }
}
