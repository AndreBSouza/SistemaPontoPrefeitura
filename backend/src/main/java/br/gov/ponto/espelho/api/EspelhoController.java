package br.gov.ponto.espelho.api;

import br.gov.ponto.espelho.CompetenciaService;
import br.gov.ponto.espelho.EspelhoService;
import br.gov.ponto.espelho.PendenciaFechamentoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/espelho")
public class EspelhoController {

    private final EspelhoService espelhoService;
    private final CompetenciaService competenciaService;
    private final PendenciaFechamentoService pendenciaFechamentoService;

    public EspelhoController(EspelhoService espelhoService, CompetenciaService competenciaService,
                            PendenciaFechamentoService pendenciaFechamentoService) {
        this.espelhoService = espelhoService;
        this.competenciaService = competenciaService;
        this.pendenciaFechamentoService = pendenciaFechamentoService;
    }

    @GetMapping
    public EspelhoResponse gerar(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return espelhoService.gerar(vinculoId, competencia);
    }

    @PostMapping("/fechar")
    public CompetenciaResponse fechar(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return CompetenciaResponse.from(competenciaService.fechar(vinculoId, competencia));
    }

    /** Fechamento de competência em lote (12.6.2): vários vínculos de uma vez. */
    @PostMapping("/fechar-lote")
    public List<CompetenciaResponse> fecharLote(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
            @Valid @RequestBody FecharLoteRequest request) {
        return competenciaService.fecharEmLote(request.vinculoIds(), competencia).stream()
                .map(CompetenciaResponse::from).toList();
    }

    /** Painel "o que falta fechar" (12.6.2): pendências de fechamento por órgão. */
    @GetMapping("/pendentes")
    public PendenciaFechamentoResponse pendentes(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return pendenciaFechamentoService.pendentes(competencia);
    }

    @PostMapping("/reabrir")
    public CompetenciaResponse reabrir(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
            @Valid @RequestBody ReaberturaRequest request) {
        return CompetenciaResponse.from(competenciaService.reabrir(vinculoId, competencia, request.motivo()));
    }

    @PostMapping("/ciencia")
    public CompetenciaResponse ciencia(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
            @RequestParam(required = false) String evidencia) {
        String ev = (evidencia == null || evidencia.isBlank()) ? "ciencia-eletronica" : evidencia;
        return CompetenciaResponse.from(competenciaService.darCiencia(vinculoId, competencia, ev));
    }
}
