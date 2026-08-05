package br.gov.ponto.jornada;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.jornada.api.CriarEscalaRequest;
import br.gov.ponto.jornada.api.EscalaResponse;
import br.gov.ponto.jornada.domain.Escala;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EscalaService {

    private final EscalaRepository escalaRepository;
    private final JornadaRepository jornadaRepository;
    private final VinculoRepository vinculoRepository;

    public EscalaService(EscalaRepository escalaRepository,
                         JornadaRepository jornadaRepository,
                         VinculoRepository vinculoRepository) {
        this.escalaRepository = escalaRepository;
        this.jornadaRepository = jornadaRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional
    public EscalaResponse atribuir(CriarEscalaRequest request) {
        UUID tenantId = TenantContext.requireCurrent();

        if (request.dataFim() != null && request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalArgumentException("dataFim nao pode ser anterior a dataInicio");
        }
        if (!vinculoRepository.existsByIdAndTenantId(request.vinculoId(), tenantId)) {
            throw new IllegalArgumentException("Vinculo inexistente no ente");
        }
        if (!jornadaRepository.existsByIdAndTenantId(request.jornadaId(), tenantId)) {
            throw new IllegalArgumentException("Jornada inexistente no ente");
        }

        boolean sobrepoe = escalaRepository.findByVinculoIdAndTenantId(request.vinculoId(), tenantId).stream()
                .anyMatch(e -> e.sobrepoeCom(request.dataInicio(), request.dataFim()));
        if (sobrepoe) {
            throw new ConflitoException("A escala se sobrepoe a uma vigencia existente do vinculo");
        }

        Escala escala = escalaRepository.save(new Escala(
                tenantId, request.vinculoId(), request.jornadaId(),
                request.dataInicio(), request.dataFim()));
        return EscalaResponse.from(escala);
    }

    /**
     * Aplicação em massa de uma jornada (template reutilizável) a vários vínculos (12.6.3).
     * Pula vínculos cuja vigência se sobreporia a uma escala existente (não falha o lote).
     */
    @Transactional
    public List<EscalaResponse> atribuirEmLote(UUID jornadaId, List<UUID> vinculoIds,
                                               LocalDate dataInicio, LocalDate dataFim) {
        List<EscalaResponse> aplicadas = new ArrayList<>();
        for (UUID vinculoId : vinculoIds) {
            try {
                aplicadas.add(atribuir(new CriarEscalaRequest(vinculoId, jornadaId, dataInicio, dataFim)));
            } catch (ConflitoException sobreposicao) {
                // vínculo já tem escala vigente no período — pula (lote robusto).
            }
        }
        return aplicadas;
    }

    /** Troca de turno: troca as jornadas atribuidas entre duas escalas. */
    @Transactional
    public void trocarTurno(UUID escalaIdA, UUID escalaIdB) {
        UUID tenantId = TenantContext.requireCurrent();
        Escala a = escalaRepository.findByIdAndTenantId(escalaIdA, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Escala inexistente: " + escalaIdA));
        Escala b = escalaRepository.findByIdAndTenantId(escalaIdB, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Escala inexistente: " + escalaIdB));
        UUID jornadaDeA = a.getJornadaId();
        a.definirJornada(b.getJornadaId());
        b.definirJornada(jornadaDeA);
        escalaRepository.save(a);
        escalaRepository.save(b);
    }

    @Transactional(readOnly = true)
    public List<EscalaResponse> listarPorVinculo(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        return escalaRepository.findByVinculoIdAndTenantId(vinculoId, tenantId).stream()
                .map(EscalaResponse::from).toList();
    }
}
