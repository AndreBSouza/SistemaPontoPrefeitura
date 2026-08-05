package br.gov.ponto.relatorios;

import br.gov.ponto.apuracao.api.ApuracaoDiaResponse;
import br.gov.ponto.apuracao.api.OcorrenciaResponse;
import br.gov.ponto.apuracao.domain.TipoOcorrencia;
import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.EspelhoService;
import br.gov.ponto.espelho.api.EspelhoResponse;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.relatorios.api.ConformidadeResponse;
import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/** Relatorios de frequencia (8.1) e evidencia de conformidade IN 008/2021 (8.4). */
@Service
public class RelatorioService {

    private final EspelhoService espelhoService;
    private final ServidorRepository servidorRepository;
    private final VinculoRepository vinculoRepository;
    private final RegistroPontoRepository registroRepository;
    private final CacheFrequencia cacheFrequencia;

    public RelatorioService(EspelhoService espelhoService,
                            ServidorRepository servidorRepository,
                            VinculoRepository vinculoRepository,
                            RegistroPontoRepository registroRepository,
                            CacheFrequencia cacheFrequencia) {
        this.espelhoService = espelhoService;
        this.servidorRepository = servidorRepository;
        this.vinculoRepository = vinculoRepository;
        this.registroRepository = registroRepository;
        this.cacheFrequencia = cacheFrequencia;
    }

    @Transactional(readOnly = true)
    public RelatorioFrequenciaResponse frequenciaMensal(UUID vinculoId, YearMonth competencia) {
        String chave = TenantContext.requireCurrent() + "|" + vinculoId + "|" + competencia;
        return cacheFrequencia.obter(chave, () -> calcularFrequencia(vinculoId, competencia));
    }

    private RelatorioFrequenciaResponse calcularFrequencia(UUID vinculoId, YearMonth competencia) {
        EspelhoResponse espelho = espelhoService.gerar(vinculoId, competencia);
        int atrasos = 0;
        int faltas = 0;
        int horaExtra = 0;
        int justificados = 0;
        for (ApuracaoDiaResponse dia : espelho.dias()) {
            if (dia.justificado()) {
                justificados++;
            }
            for (OcorrenciaResponse oc : dia.ocorrencias()) {
                if (oc.tipo() == TipoOcorrencia.ATRASO) {
                    atrasos++;
                } else if (oc.tipo() == TipoOcorrencia.FALTA) {
                    faltas++;
                } else if (oc.tipo() == TipoOcorrencia.HORA_EXTRA) {
                    horaExtra += oc.minutos();
                }
            }
        }
        return new RelatorioFrequenciaResponse(vinculoId, competencia.toString(),
                espelho.totalMinutosTrabalhados(), espelho.totalMinutosEsperados(),
                atrasos, faltas, horaExtra, justificados);
    }

    @Transactional(readOnly = true)
    public String exportarCsv(UUID vinculoId, YearMonth competencia) {
        RelatorioFrequenciaResponse r = frequenciaMensal(vinculoId, competencia);
        return "vinculo;competencia;trabalhados;esperados;atrasos;faltas;horaExtra;justificados\n"
                + String.join(";",
                String.valueOf(r.vinculoId()), r.competencia(),
                String.valueOf(r.totalMinutosTrabalhados()), String.valueOf(r.totalMinutosEsperados()),
                String.valueOf(r.qtdAtrasos()), String.valueOf(r.qtdFaltas()),
                String.valueOf(r.minutosHoraExtra()), String.valueOf(r.diasJustificados()))
                + "\n";
    }

    @Transactional(readOnly = true)
    public ConformidadeResponse conformidadeIn008(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);
        Instant inicio = periodo[0];
        Instant fim = periodo[1];
        long servidores = servidorRepository.findByTenantId(tenantId).size();
        long vinculos = vinculoRepository.findByTenantId(tenantId).size();
        long registros = registroRepository.countByTenantIdAndDataHoraServidorBetween(tenantId, inicio, fim);
        return new ConformidadeResponse(competencia.toString(), servidores, vinculos, registros,
                "Evidencia de controle de frequencia (IN 008/2021 - Atos de Pessoal)");
    }
}
