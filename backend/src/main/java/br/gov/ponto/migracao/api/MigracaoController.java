package br.gov.ponto.migracao.api;

import br.gov.ponto.migracao.MigracaoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Migração de dados de sistemas anteriores (12.5.3, parte codável) — sob /api/migracao (RH/admin). */
@RestController
@RequestMapping("/api/migracao")
public class MigracaoController {

    private final MigracaoService migracaoService;

    public MigracaoController(MigracaoService migracaoService) {
        this.migracaoService = migracaoService;
    }

    /**
     * Importa o saldo inicial de banco de horas de um sistema anterior (CSV
     * {@code matricula;saldoMinutos}). {@code data} = data de referência do saldo. Idempotente.
     */
    @PostMapping(value = "/banco-horas", consumes = {"text/csv", "text/plain"})
    public MigracaoSaldoResponse importarBancoHoras(
            @RequestBody String csv,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return migracaoService.importarSaldoBancoHoras(csv, data);
    }
}
