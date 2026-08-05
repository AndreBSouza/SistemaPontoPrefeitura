package br.gov.ponto.calendario.api;

import br.gov.ponto.calendario.CalendarioService;
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

/** Calendário oficial do município (feriados, pontos facultativos, abonos coletivos). */
@RestController
@RequestMapping("/api/calendario")
public class CalendarioController {

    private final CalendarioService calendarioService;

    public CalendarioController(CalendarioService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @PostMapping
    public ResponseEntity<EventoCalendarioResponse> criar(@Valid @RequestBody CriarEventoRequest request) {
        var e = calendarioService.criar(request.data(), request.tipo(), request.descricao(), request.lotacaoId());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventoCalendarioResponse.from(e));
    }

    @GetMapping
    public List<EventoCalendarioResponse> listar(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia) {
        return calendarioService.listar(competencia).stream().map(EventoCalendarioResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        calendarioService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
