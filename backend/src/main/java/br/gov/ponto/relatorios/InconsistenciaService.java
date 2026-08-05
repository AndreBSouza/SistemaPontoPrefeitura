package br.gov.ponto.relatorios;

import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.relatorios.api.InconsistenciasResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Detecção de inconsistências de jornada (12.4.15): aponta dias com número ímpar de
 * marcações — intervalo aberto, sinal de esquecimento de batida (entrada sem saída, etc.).
 */
@Service
public class InconsistenciaService {

    private final RegistroPontoRepository registroRepository;
    private final ServidorRepository servidorRepository;
    private final VinculoRepository vinculoRepository;

    public InconsistenciaService(RegistroPontoRepository registroRepository,
                                 ServidorRepository servidorRepository, VinculoRepository vinculoRepository) {
        this.registroRepository = registroRepository;
        this.servidorRepository = servidorRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional(readOnly = true)
    public InconsistenciasResponse detectar(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);

        Map<UUID, String> nomes = servidorRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Servidor::getId, Servidor::getNome, (a, b) -> a));
        Map<UUID, UUID> servidorDoVinculo = vinculoRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Vinculo::getId, Vinculo::getServidorId, (a, b) -> a));

        // Conta marcações por (vínculo, dia).
        Map<UUID, Map<LocalDate, Integer>> porVinculoDia = new LinkedHashMap<>();
        for (RegistroPonto r : registroRepository
                .findByTenantIdAndDataHoraServidorBetweenOrderByNsr(tenantId, periodo[0], periodo[1])) {
            LocalDate dia = r.getDataHoraServidor().atZone(TempoMunicipal.ZONE).toLocalDate();
            porVinculoDia.computeIfAbsent(r.getVinculoId(), k -> new LinkedHashMap<>())
                    .merge(dia, 1, Integer::sum);
        }

        List<InconsistenciasResponse.Item> itens = new ArrayList<>();
        for (Map.Entry<UUID, Map<LocalDate, Integer>> e : porVinculoDia.entrySet()) {
            String servidor = nomes.getOrDefault(servidorDoVinculo.get(e.getKey()), "?");
            for (Map.Entry<LocalDate, Integer> dia : e.getValue().entrySet()) {
                if (dia.getValue() % 2 != 0) {
                    itens.add(new InconsistenciasResponse.Item(e.getKey(), servidor, dia.getKey(),
                            dia.getValue(), "Número ímpar de marcações (possível esquecimento)"));
                }
            }
        }
        return new InconsistenciasResponse(competencia.toString(), itens);
    }
}
