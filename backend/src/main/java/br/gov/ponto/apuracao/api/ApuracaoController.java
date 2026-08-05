package br.gov.ponto.apuracao.api;

import br.gov.ponto.apuracao.ApuracaoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/apuracao")
public class ApuracaoController {

    private final ApuracaoService apuracaoService;

    public ApuracaoController(ApuracaoService apuracaoService) {
        this.apuracaoService = apuracaoService;
    }

    @GetMapping("/dia")
    public ApuracaoDiaResponse apurarDia(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ApuracaoDiaResponse.from(apuracaoService.apurarDia(vinculoId, data));
    }
}
