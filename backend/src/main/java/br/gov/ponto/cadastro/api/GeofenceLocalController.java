package br.gov.ponto.cadastro.api;

import br.gov.ponto.cadastro.GeofenceLocalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Áreas de referência adicionais do órgão (multi-geofence / locais volantes, 12.3.10). */
@RestController
@RequestMapping("/api/lotacoes/{lotacaoId}/locais")
public class GeofenceLocalController {

    private final GeofenceLocalService geofenceLocalService;

    public GeofenceLocalController(GeofenceLocalService geofenceLocalService) {
        this.geofenceLocalService = geofenceLocalService;
    }

    @GetMapping
    public List<GeofenceLocalResponse> listar(@PathVariable UUID lotacaoId) {
        return geofenceLocalService.listar(lotacaoId);
    }

    @PostMapping
    public ResponseEntity<GeofenceLocalResponse> criar(@PathVariable UUID lotacaoId,
                                                       @Valid @RequestBody CriarGeofenceLocalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(geofenceLocalService.criar(lotacaoId, request));
    }

    @DeleteMapping("/{localId}")
    public ResponseEntity<Void> remover(@PathVariable UUID lotacaoId, @PathVariable UUID localId) {
        geofenceLocalService.remover(lotacaoId, localId);
        return ResponseEntity.noContent().build();
    }
}
