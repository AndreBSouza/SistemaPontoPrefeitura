package br.gov.ponto.relatorios;

import br.gov.ponto.cadastro.LotacaoRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.relatorios.api.BiExecutivoResponse;
import br.gov.ponto.relatorios.api.RelatorioFrequenciaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BI executivo por secretaria (12.3.2): agrega, por órgão, vínculos, faltas (absenteísmo),
 * hora extra (custo) e vínculos sem batida no período. Itera a frequência mensal por vínculo
 * (custoso em entes grandes — candidato a view materializada/cache em produção).
 */
@Service
public class BiExecutivoService {

    private final LotacaoRepository lotacaoRepository;
    private final VinculoRepository vinculoRepository;
    private final RelatorioService relatorioService;
    private final RegistroPontoRepository registroRepository;

    public BiExecutivoService(LotacaoRepository lotacaoRepository, VinculoRepository vinculoRepository,
                              RelatorioService relatorioService, RegistroPontoRepository registroRepository) {
        this.lotacaoRepository = lotacaoRepository;
        this.vinculoRepository = vinculoRepository;
        this.relatorioService = relatorioService;
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public BiExecutivoResponse porOrgao(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);

        List<BiExecutivoResponse.OrgaoBi> linhas = new ArrayList<>();
        int totVinculos = 0;
        int totFaltas = 0;
        int totHoraExtra = 0;
        int totFantasmas = 0;

        for (Lotacao orgao : lotacaoRepository.findByTenantId(tenantId)) {
            List<Vinculo> vinculos = vinculoRepository.findByLotacaoIdInAndTenantId(List.of(orgao.getId()), tenantId)
                    .stream().filter(Vinculo::isAtivo).toList();
            int faltas = 0;
            int horaExtra = 0;
            int fantasmas = 0;
            for (Vinculo v : vinculos) {
                RelatorioFrequenciaResponse freq = relatorioService.frequenciaMensal(v.getId(), competencia);
                faltas += freq.qtdFaltas();
                horaExtra += freq.minutosHoraExtra();
                boolean semBatida = registroRepository
                        .findByVinculoIdAndTenantIdAndDataHoraServidorBetweenOrderByDataHoraServidor(
                                v.getId(), tenantId, periodo[0], periodo[1]).isEmpty();
                if (semBatida) {
                    fantasmas++;
                }
            }
            linhas.add(new BiExecutivoResponse.OrgaoBi(orgao.getId(), orgao.getNome(),
                    vinculos.size(), faltas, horaExtra, fantasmas));
            totVinculos += vinculos.size();
            totFaltas += faltas;
            totHoraExtra += horaExtra;
            totFantasmas += fantasmas;
        }

        return new BiExecutivoResponse(competencia.toString(), totVinculos, totFaltas,
                totHoraExtra, totFantasmas, linhas);
    }
}
