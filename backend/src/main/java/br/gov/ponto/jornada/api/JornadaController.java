package br.gov.ponto.jornada.api;

import br.gov.ponto.jornada.JornadaService;
import br.gov.ponto.jornada.PisoMagisterioService;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.api.HorarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jornadas")
public class JornadaController {

    private final JornadaService jornadaService;
    private final PisoMagisterioService pisoMagisterioService;

    public JornadaController(JornadaService jornadaService, PisoMagisterioService pisoMagisterioService) {
        this.jornadaService = jornadaService;
        this.pisoMagisterioService = pisoMagisterioService;
    }

    @PostMapping
    public ResponseEntity<JornadaResponse> criar(@Valid @RequestBody CriarJornadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jornadaService.criar(request));
    }

    @GetMapping
    public List<JornadaResponse> listar() {
        return jornadaService.listar();
    }

    /** Conformidade da hora-atividade do magistério (Lei do Piso): jornadas que atingem ≥ 1/3. */
    @GetMapping("/piso-magisterio")
    public List<PisoMagisterioResponse> pisoMagisterio() {
        return pisoMagisterioService.avaliar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JornadaResponse> buscar(@PathVariable UUID id) {
        return jornadaService.buscar(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/horarios")
    public List<HorarioResponse> definirHorarios(@PathVariable UUID id,
                                                 @Valid @RequestBody List<HorarioRequest> horarios) {
        return jornadaService.definirHorarios(id, horarios);
    }

    @GetMapping("/{id}/horarios")
    public List<HorarioResponse> listarHorarios(@PathVariable UUID id) {
        return jornadaService.buscarHorarios(id);
    }
}

