package br.gov.ponto.bancohoras.api;

import br.gov.ponto.bancohoras.BancoHorasService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/banco-horas")
public class BancoHorasController {

    private final BancoHorasService bancoHorasService;

    public BancoHorasController(BancoHorasService bancoHorasService) {
        this.bancoHorasService = bancoHorasService;
    }

    @GetMapping("/saldo")
    public SaldoResponse saldo(@RequestParam UUID vinculoId) {
        return new SaldoResponse(vinculoId, bancoHorasService.saldo(vinculoId));
    }

    @PostMapping("/apurar-dia")
    public SaldoResponse apurarDia(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return new SaldoResponse(vinculoId, bancoHorasService.lancarApuracaoDoDia(vinculoId, data));
    }

    @PostMapping("/compensar")
    public SaldoResponse compensar(@Valid @RequestBody CompensarRequest request) {
        return new SaldoResponse(request.vinculoId(),
                bancoHorasService.compensar(request.vinculoId(), request.minutos(),
                        request.data(), request.descricao()));
    }

    @PostMapping("/ajuste")
    public SaldoResponse ajuste(@Valid @RequestBody AjusteRequest request) {
        return new SaldoResponse(request.vinculoId(),
                bancoHorasService.ajustar(request.vinculoId(), request.data(),
                        request.minutos(), request.descricao()));
    }

    @PostMapping("/prescrever")
    public SaldoResponse prescrever(
            @RequestParam UUID vinculoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataLimite) {
        return new SaldoResponse(vinculoId, bancoHorasService.prescrever(vinculoId, dataLimite));
    }
}
