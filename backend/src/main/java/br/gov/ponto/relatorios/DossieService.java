package br.gov.ponto.relatorios;

import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.relatorios.api.AfdResponse;
import br.gov.ponto.relatorios.api.ConformidadeResponse;
import br.gov.ponto.relatorios.api.DossieResponse;
import br.gov.ponto.relatorios.api.IndicadoresResponse;
import br.gov.ponto.relatorios.api.IntegridadeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Monta o dossiê de conformidade (escudo jurídico pró-TCM): compõe IN 008, AFD (hash),
 * verificação de integridade da cadeia, indicadores e o prazo de submissão num único pacote
 * de defesa. Reutiliza os serviços existentes — não recalcula nada.
 */
@Service
public class DossieService {

    /** Prazo ilustrativo de envio ao controle: dia 15 do mês seguinte à competência. */
    private static final int DIA_PRAZO = 15;

    private final RelatorioService relatorioService;
    private final AfdService afdService;
    private final IntegridadeService integridadeService;
    private final IndicadoresService indicadoresService;

    public DossieService(RelatorioService relatorioService, AfdService afdService,
                         IntegridadeService integridadeService, IndicadoresService indicadoresService) {
        this.relatorioService = relatorioService;
        this.afdService = afdService;
        this.integridadeService = integridadeService;
        this.indicadoresService = indicadoresService;
    }

    @Transactional(readOnly = true)
    public DossieResponse gerar(YearMonth competencia) {
        ConformidadeResponse conformidade = relatorioService.conformidadeIn008(competencia);
        AfdResponse afd = afdService.gerar(competencia);
        IntegridadeResponse integridade = integridadeService.verificar(competencia);
        IndicadoresResponse indicadores = indicadoresService.obter(competencia);

        LocalDate prazo = competencia.plusMonths(1).atDay(DIA_PRAZO);
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(TempoMunicipal.ZONE), prazo);

        List<String> escudos = List.of(
                "AFD — Portaria MTP 671/2021 (largura fixa, hash SHA-256)",
                "AEJ — Arquivo Eletrônico de Jornada",
                "Cadeia de hash do NSR (tamper-evidence, REP-P)",
                "Conformidade IN 008/2021 (TCM-GO)",
                "LGPD — consentimento + DPIA/RIPD",
                "Trilha de auditoria imutável");

        return new DossieResponse(competencia.toString(), conformidade, afd.hashSha256(),
                afd.totalRegistros(), integridade.integra(), integridade.detalhe(),
                indicadores, prazo, diasRestantes, escudos);
    }
}
