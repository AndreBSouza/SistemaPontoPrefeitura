package br.gov.ponto.sobreaviso;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.sobreaviso.api.RegistrarSobreavisoRequest;
import br.gov.ponto.sobreaviso.api.SobreavisoResponse;
import br.gov.ponto.sobreaviso.domain.Sobreaviso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Sobreaviso (on-call, 12.4.3): o servidor fica à disposição fora do expediente. As horas são
 * contadas à parte (não interferem na apuração normal) e somadas por competência para a folha,
 * que paga em rubrica/percentual próprio (ex.: 1/3 da hora).
 */
@Service
public class SobreavisoService {

    private final SobreavisoRepository repository;
    private final VinculoRepository vinculoRepository;
    private final AuditoriaService auditoriaService;

    public SobreavisoService(SobreavisoRepository repository,
                             VinculoRepository vinculoRepository,
                             AuditoriaService auditoriaService) {
        this.repository = repository;
        this.vinculoRepository = vinculoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public SobreavisoResponse registrar(RegistrarSobreavisoRequest request) {
        UUID tenantId = TenantContext.requireCurrent();
        if (!vinculoRepository.existsByIdAndTenantId(request.vinculoId(), tenantId)) {
            throw new RecursoNaoEncontradoException("Vínculo inexistente no ente");
        }
        Sobreaviso salvo = repository.save(new Sobreaviso(tenantId, request.vinculoId(),
                request.data(), request.minutos(), request.observacao()));
        auditoriaService.registrar("SOBREAVISO_REGISTRADO", "sobreaviso", salvo.getId().toString(),
                request.minutos() + " min em " + request.data());
        return SobreavisoResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<SobreavisoResponse> listarPorVinculo(UUID vinculoId) {
        return repository.findByVinculoIdAndTenantIdOrderByDataDesc(vinculoId, TenantContext.requireCurrent())
                .stream().map(SobreavisoResponse::from).toList();
    }

    /** Total de minutos de sobreaviso do vínculo na competência (para a folha). */
    @Transactional(readOnly = true)
    public int totalMinutos(UUID vinculoId, YearMonth competencia) {
        return repository.findByVinculoIdAndTenantIdAndDataBetween(vinculoId,
                        TenantContext.requireCurrent(), competencia.atDay(1), competencia.atEndOfMonth())
                .stream().mapToInt(Sobreaviso::getMinutos).sum();
    }

    @Transactional
    public void remover(UUID id) {
        UUID tenantId = TenantContext.requireCurrent();
        Sobreaviso s = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sobreaviso inexistente"));
        repository.delete(s);
        auditoriaService.registrar("SOBREAVISO_REMOVIDO", "sobreaviso", id.toString(),
                s.getMinutos() + " min em " + s.getData());
    }
}
