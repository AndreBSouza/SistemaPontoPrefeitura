package br.gov.ponto.relatorios;

import br.gov.ponto.cadastro.ServidorRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Servidor;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.EscalaRepository;
import br.gov.ponto.jornada.JornadaHorarioRepository;
import br.gov.ponto.registro.RegistroPontoRepository;
import br.gov.ponto.relatorios.api.DeteccaoResponse;
import br.gov.ponto.relatorios.domain.DetectorAcumulo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Varredura de irregularidades para o controle interno (12.5.2): cruza vínculos/jornadas
 * sobrepostas (acúmulo ilícito de cargos) e aponta vínculos ativos sem nenhuma batida no
 * período ("servidor fantasma"). Regra de sobreposição delegada a {@link DetectorAcumulo}.
 */
@Service
public class DeteccaoService {

    private final VinculoRepository vinculoRepository;
    private final ServidorRepository servidorRepository;
    private final EscalaRepository escalaRepository;
    private final JornadaHorarioRepository horarioRepository;
    private final RegistroPontoRepository registroRepository;

    public DeteccaoService(VinculoRepository vinculoRepository, ServidorRepository servidorRepository,
                           EscalaRepository escalaRepository, JornadaHorarioRepository horarioRepository,
                           RegistroPontoRepository registroRepository) {
        this.vinculoRepository = vinculoRepository;
        this.servidorRepository = servidorRepository;
        this.escalaRepository = escalaRepository;
        this.horarioRepository = horarioRepository;
        this.registroRepository = registroRepository;
    }

    @Transactional(readOnly = true)
    public DeteccaoResponse detectar(YearMonth competencia) {
        UUID tenantId = TenantContext.requireCurrent();
        Instant[] periodo = TempoMunicipal.intervaloDaCompetencia(competencia);
        LocalDate referencia = competencia.atEndOfMonth();

        List<Vinculo> ativos = vinculoRepository.findByTenantId(tenantId).stream()
                .filter(Vinculo::isAtivo).toList();
        Map<UUID, String> nomes = servidorRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Servidor::getId, Servidor::getNome, (a, b) -> a));

        List<DeteccaoResponse.Acumulo> acumulos = detectarAcumulos(ativos, tenantId, referencia, nomes);
        List<DeteccaoResponse.Fantasma> fantasmas = detectarFantasmas(ativos, tenantId, periodo, nomes);
        return new DeteccaoResponse(acumulos, fantasmas);
    }

    private List<DeteccaoResponse.Acumulo> detectarAcumulos(List<Vinculo> ativos, UUID tenantId,
                                                            LocalDate referencia, Map<UUID, String> nomes) {
        List<DeteccaoResponse.Acumulo> acumulos = new ArrayList<>();
        Map<UUID, List<Vinculo>> porServidor = ativos.stream()
                .collect(Collectors.groupingBy(Vinculo::getServidorId));
        for (Map.Entry<UUID, List<Vinculo>> e : porServidor.entrySet()) {
            List<Vinculo> vs = e.getValue();
            if (vs.size() < 2) {
                continue;
            }
            for (int i = 0; i < vs.size(); i++) {
                List<DetectorAcumulo.Janela> a = janelas(vs.get(i).getId(), tenantId, referencia);
                for (int j = i + 1; j < vs.size(); j++) {
                    List<DetectorAcumulo.Janela> b = janelas(vs.get(j).getId(), tenantId, referencia);
                    if (DetectorAcumulo.haConflito(a, b)) {
                        acumulos.add(new DeteccaoResponse.Acumulo(e.getKey(),
                                nomes.getOrDefault(e.getKey(), "?"), vs.get(i).getId(), vs.get(j).getId()));
                    }
                }
            }
        }
        return acumulos;
    }

    private List<DeteccaoResponse.Fantasma> detectarFantasmas(List<Vinculo> ativos, UUID tenantId,
                                                              Instant[] periodo, Map<UUID, String> nomes) {
        List<DeteccaoResponse.Fantasma> fantasmas = new ArrayList<>();
        for (Vinculo v : ativos) {
            boolean semBatida = registroRepository
                    .findByVinculoIdAndTenantIdAndDataHoraServidorBetweenOrderByDataHoraServidor(
                            v.getId(), tenantId, periodo[0], periodo[1]).isEmpty();
            if (semBatida) {
                fantasmas.add(new DeteccaoResponse.Fantasma(v.getServidorId(),
                        nomes.getOrDefault(v.getServidorId(), "?"), v.getId(), v.getMatricula()));
            }
        }
        return fantasmas;
    }

    /** Janelas de horário da jornada vigente (na data de referência) do vínculo. */
    private List<DetectorAcumulo.Janela> janelas(UUID vinculoId, UUID tenantId, LocalDate referencia) {
        return escalaRepository.findByVinculoIdAndTenantId(vinculoId, tenantId).stream()
                .filter(es -> es.vigenteEm(referencia))
                .findFirst()
                .map(es -> horarioRepository.findByJornadaIdAndTenantId(es.getJornadaId(), tenantId).stream()
                        .map(h -> new DetectorAcumulo.Janela(h.getDiaSemana(),
                                minuto(h.getHoraEntrada()), minuto(h.getHoraSaida())))
                        .toList())
                .orElseGet(List::of);
    }

    private int minuto(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}
