package br.gov.ponto.auditoria.api;

import br.gov.ponto.auditoria.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public List<AuditoriaResponse> listar() {
        return auditoriaService.listar().stream().map(AuditoriaResponse::from).toList();
    }

    @GetMapping("/entidade")
    public List<AuditoriaResponse> porEntidade(@RequestParam String entidade,
                                               @RequestParam String id) {
        return auditoriaService.listarPorEntidade(entidade, id).stream()
                .map(AuditoriaResponse::from).toList();
    }
}
