package br.gov.ponto.jornada.api;

import br.gov.ponto.jornada.EscalaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/escalas")
public class EscalaController {

    private final EscalaService escalaService;

    public EscalaController(EscalaService escalaService) {
        this.escalaService = escalaService;
    }

    @PostMapping
    public ResponseEntity<EscalaResponse> atribuir(@Valid @RequestBody CriarEscalaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(escalaService.atribuir(request));
    }

    @GetMapping
    public List<EscalaResponse> listarPorVinculo(@RequestParam UUID vinculoId) {
        return escalaService.listarPorVinculo(vinculoId);
    }

    @PostMapping("/trocar-turno")
    public ResponseEntity<Void> trocarTurno(@RequestParam UUID escalaA, @RequestParam UUID escalaB) {
        escalaService.trocarTurno(escalaA, escalaB);
        return ResponseEntity.noContent().build();
    }

    /** Aplicação em massa de uma jornada (template) a vários vínculos de uma vez (12.6.3). */
    @PostMapping("/lote")
    public List<EscalaResponse> atribuirEmLote(@Valid @RequestBody AtribuirLoteRequest request) {
        return escalaService.atribuirEmLote(request.jornadaId(), request.vinculoIds(),
                request.dataInicio(), request.dataFim());
    }

    public record AtribuirLoteRequest(
            @NotNull UUID jornadaId,
            @NotEmpty List<UUID> vinculoIds,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
    }
}
