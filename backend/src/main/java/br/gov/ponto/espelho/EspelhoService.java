package br.gov.ponto.espelho;

import br.gov.ponto.apuracao.ApuracaoService;
import br.gov.ponto.apuracao.api.ApuracaoDiaResponse;
import br.gov.ponto.apuracao.domain.ApuracaoDia;
import br.gov.ponto.espelho.api.EspelhoResponse;
import br.gov.ponto.espelho.domain.Competencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Geracao do espelho de ponto mensal (consolida a apuracao diaria + status da competencia). */
@Service
public class EspelhoService {

    private final ApuracaoService apuracaoService;
    private final CompetenciaService competenciaService;

    public EspelhoService(ApuracaoService apuracaoService, CompetenciaService competenciaService) {
        this.apuracaoService = apuracaoService;
        this.competenciaService = competenciaService;
    }

    @Transactional(readOnly = true)
    public EspelhoResponse gerar(UUID vinculoId, YearMonth competencia) {
        List<ApuracaoDiaResponse> dias = new ArrayList<>();
        int totalTrabalhados = 0;
        int totalEsperados = 0;
        for (int dia = 1; dia <= competencia.lengthOfMonth(); dia++) {
            LocalDate data = competencia.atDay(dia);
            ApuracaoDia apuracao = apuracaoService.apurarDia(vinculoId, data);
            dias.add(ApuracaoDiaResponse.from(apuracao));
            totalTrabalhados += apuracao.minutosTrabalhados();
            totalEsperados += apuracao.minutosEsperados();
        }
        Optional<Competencia> comp = competenciaService.buscar(vinculoId, competencia);
        String status = comp.map(c -> c.getStatus().name()).orElse("ABERTA");
        Instant cienciaEm = comp.map(Competencia::getCienciaEm).orElse(null);
        return new EspelhoResponse(vinculoId, competencia.toString(), status, cienciaEm,
                totalTrabalhados, totalEsperados, dias);
    }
}
