package br.gov.ponto.jornada;

import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.api.CriarJornadaRequest;
import br.gov.ponto.jornada.api.HorarioRequest;
import br.gov.ponto.jornada.api.HorarioResponse;
import br.gov.ponto.jornada.api.JornadaResponse;
import br.gov.ponto.jornada.domain.Jornada;
import br.gov.ponto.jornada.domain.JornadaHorario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JornadaService {

    private final JornadaRepository jornadaRepository;
    private final JornadaHorarioRepository horarioRepository;

    public JornadaService(JornadaRepository jornadaRepository, JornadaHorarioRepository horarioRepository) {
        this.jornadaRepository = jornadaRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional
    public JornadaResponse criar(CriarJornadaRequest request) {
        UUID tenantId = TenantContext.requireCurrent();
        if (jornadaRepository.existsByTenantIdAndNome(tenantId, request.nome())) {
            throw new ConflitoException("Ja existe jornada com o nome " + request.nome());
        }
        Jornada jornada = new Jornada(
                tenantId, request.nome(), request.tipo(),
                request.cargaHorariaSemanalMin(), request.toleranciaMin(), request.intervaloMin());
        jornada.setHoraAtividadeMin(request.horaAtividadeMin());
        jornada = jornadaRepository.save(jornada);
        return JornadaResponse.from(jornada);
    }

    @Transactional
    public List<HorarioResponse> definirHorarios(UUID jornadaId, List<HorarioRequest> horarios) {
        UUID tenantId = TenantContext.requireCurrent();
        if (!jornadaRepository.existsByIdAndTenantId(jornadaId, tenantId)) {
            throw new IllegalArgumentException("Jornada inexistente no ente");
        }
        horarioRepository.deleteByJornadaIdAndTenantId(jornadaId, tenantId);
        for (HorarioRequest h : horarios) {
            if (h.horaSaida().isBefore(h.horaEntrada())) {
                throw new IllegalArgumentException("horaSaida anterior a horaEntrada no dia " + h.diaSemana());
            }
            horarioRepository.save(new JornadaHorario(
                    tenantId, jornadaId, h.diaSemana(), h.horaEntrada(), h.horaSaida()));
        }
        return buscarHorarios(jornadaId);
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> buscarHorarios(UUID jornadaId) {
        UUID tenantId = TenantContext.requireCurrent();
        return horarioRepository.findByJornadaIdAndTenantId(jornadaId, tenantId).stream()
                .map(h -> new HorarioResponse(h.getDiaSemana(), h.getHoraEntrada(), h.getHoraSaida()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JornadaResponse> listar() {
        UUID tenantId = TenantContext.requireCurrent();
        return jornadaRepository.findByTenantId(tenantId).stream().map(JornadaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<JornadaResponse> buscar(UUID id) {
        UUID tenantId = TenantContext.requireCurrent();
        return jornadaRepository.findByIdAndTenantId(id, tenantId).map(JornadaResponse::from);
    }
}
