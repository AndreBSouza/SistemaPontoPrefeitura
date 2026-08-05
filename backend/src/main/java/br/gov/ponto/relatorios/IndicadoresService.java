package br.gov.ponto.relatorios;

import br.gov.ponto.ativacao.DispositivoRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.relatorios.api.IndicadoresResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Indicadores de gestão e adesão do ente — base do painel do gestor e do
 * acompanhamento da implantação gradual (piloto por órgão).
 */
@Service
public class IndicadoresService {

    private final VinculoRepository vinculoRepository;
    private final DispositivoRepository dispositivoRepository;
    private final RegistroPontoRepository registroRepository;

    public IndicadoresService(VinculoRepository vinculoRepository,
                              DispositivoRepository dispositivoRepository,
                              RegistroPontoRepository registroRepository) {
        this.vinculoRepository = vinculoRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public IndicadoresResponse obter(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);

        int totalVinculos = vinculoRepository.findByTenantId(tenantId).size();
        long dispositivosAtivos = dispositivoRepository.countByTenantIdAndAtivoTrue(tenantId);
        long registros = registroRepository.countByTenantIdAndDataHoraServidorBetween(tenantId, periodo[0], periodo[1]);
        int adesao = totalVinculos > 0 ? (int) Math.round(100.0 * dispositivosAtivos / totalVinculos) : 0;

        return new IndicadoresResponse(totalVinculos, dispositivosAtivos, adesao, registros);
    }
}
