package br.gov.ponto.lgpd.api;

import br.gov.ponto.lgpd.LgpdService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/lgpd")
public class LgpdController {

    private final LgpdService lgpdService;

    public LgpdController(LgpdService lgpdService) {
        this.lgpdService = lgpdService;
    }

    @PostMapping("/consentimento")
    public ResponseEntity<Void> consentimento(@Valid @RequestBody ConsentimentoRequest request) {
        lgpdService.registrarConsentimento(request.servidorId(), request.finalidade(), request.concedido());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/titular/{servidorId}/exportar")
    public ExportacaoTitularResponse exportar(@PathVariable UUID servidorId) {
        return lgpdService.exportarDadosTitular(servidorId);
    }

    @PostMapping("/titular/{servidorId}/eliminar")
    public EliminacaoResponse eliminar(@PathVariable UUID servidorId) {
        return lgpdService.eliminarDadosTitular(servidorId);
    }
}
