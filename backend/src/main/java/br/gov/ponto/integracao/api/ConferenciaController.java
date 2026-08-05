package br.gov.ponto.integracao.api;

import br.gov.ponto.integracao.ConferenciaFolhaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Conferência cruzada folha × frequência (12.6.12). */
@RestController
@RequestMapping("/api/integracoes/conferencia")
public class ConferenciaController {

    private final ConferenciaFolhaService conferenciaFolhaService;

    public ConferenciaController(ConferenciaFolhaService conferenciaFolhaService) {
        this.conferenciaFolhaService = conferenciaFolhaService;
    }

    @PostMapping
    public ConferenciaResponse conferir(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
            @Valid @RequestBody ConferenciaRequest request) {
        return conferenciaFolhaService.conferir(competencia, request);
    }
}
