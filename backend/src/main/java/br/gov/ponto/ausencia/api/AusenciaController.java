package br.gov.ponto.ausencia.api;

import br.gov.ponto.ausencia.AusenciaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Gestão de férias e licenças (programação + cobertura da equipe). */
@RestController
@RequestMapping("/api/ausencias")
public class AusenciaController {

    private final AusenciaService ausenciaService;

    public AusenciaController(AusenciaService ausenciaService) {
        this.ausenciaService = ausenciaService;
    }

    @PostMapping
    public ResponseEntity<AusenciaResponse> agendar(@Valid @RequestBody AgendarAusenciaRequest request) {
        var a = ausenciaService.agendar(request.vinculoId(), request.tipo(),
                request.dataInicio(), request.dataFim(), request.observacao());
        return ResponseEntity.status(HttpStatus.CREATED).body(AusenciaResponse.from(a));
    }

    @GetMapping
    public List<AusenciaResponse> listar(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return ausenciaService.listar(competencia).stream().map(AusenciaResponse::from).toList();
    }

    /** Cobertura da equipe de um órgão na competência (quem está ausente e quando). */
    @GetMapping("/cobertura")
    public CoberturaResponse cobertura(
            @RequestParam UUID lotacaoId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return ausenciaService.cobertura(lotacaoId, competencia);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        ausenciaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
