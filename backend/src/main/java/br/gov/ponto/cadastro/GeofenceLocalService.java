package br.gov.ponto.cadastro;

import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.cadastro.api.CriarGeofenceLocalRequest;
import br.gov.ponto.cadastro.api.GeofenceLocalResponse;
import br.gov.ponto.cadastro.domain.Geofence;
import br.gov.ponto.cadastro.domain.GeofenceLocal;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Áreas de referência adicionais por órgão (multi-geofence / locais volantes, 12.3.10).
 * Cada órgão pode ter várias áreas; a batida é "fora da área" só quando está fora de todas
 * (o cálculo combina estas áreas com a cerca primária do órgão — ver {@code RegistroService}).
 * Apenas verificação do administrador: nunca bloqueia nem alerta o servidor.
 */
@Service
public class GeofenceLocalService {

    private final GeofenceLocalRepository repository;
    private final LotacaoRepository lotacaoRepository;
    private final VinculoRepository vinculoRepository;
    private final AuditoriaService auditoriaService;

    public GeofenceLocalService(GeofenceLocalRepository repository,
                                LotacaoRepository lotacaoRepository,
                                VinculoRepository vinculoRepository,
                                AuditoriaService auditoriaService) {
        this.repository = repository;
        this.lotacaoRepository = lotacaoRepository;
        this.vinculoRepository = vinculoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public GeofenceLocalResponse criar(UUID lotacaoId, CriarGeofenceLocalRequest request) {
        UUID tenantId = TenantContext.requireCurrent();
        exigirLotacao(lotacaoId, tenantId);
        GeofenceLocal salvo = repository.save(new GeofenceLocal(tenantId, lotacaoId,
                request.nome(), request.latitude(), request.longitude(), request.raioMetros()));
        auditoriaService.registrar("GEOFENCE_LOCAL_CRIADO", "lotacao", lotacaoId.toString(),
                "area \"" + request.nome() + "\" (" + request.latitude() + "," + request.longitude()
                        + " r=" + request.raioMetros() + "m)");
        return GeofenceLocalResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<GeofenceLocalResponse> listar(UUID lotacaoId) {
        UUID tenantId = TenantContext.requireCurrent();
        return repository.findByTenantIdAndLotacaoIdOrderByNome(tenantId, lotacaoId).stream()
                .map(GeofenceLocalResponse::from).toList();
    }

    @Transactional
    public void remover(UUID lotacaoId, UUID localId) {
        UUID tenantId = TenantContext.requireCurrent();
        GeofenceLocal local = repository.findByIdAndTenantId(localId, tenantId)
                .filter(g -> g.getLotacaoId().equals(lotacaoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Área inexistente no órgão"));
        repository.delete(local);
        auditoriaService.registrar("GEOFENCE_LOCAL_REMOVIDO", "lotacao", lotacaoId.toString(),
                "area \"" + local.getNome() + "\"");
    }

    /**
     * Áreas de referência do órgão do vínculo, como regras de domínio (cercas). Usado na batida
     * para decidir se a localização está fora de todas as áreas. Vazio se o vínculo não tem órgão.
     */
    @Transactional(readOnly = true)
    public List<Geofence> areasDoVinculo(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        UUID lotacaoId = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .map(Vinculo::getLotacaoId).orElse(null);
        if (lotacaoId == null) {
            return List.of();
        }
        return repository.findByTenantIdAndLotacaoIdOrderByNome(tenantId, lotacaoId).stream()
                .map(GeofenceLocal::paraDominio).toList();
    }

    private void exigirLotacao(UUID lotacaoId, UUID tenantId) {
        if (lotacaoRepository.findByIdAndTenantId(lotacaoId, tenantId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Lotação inexistente no ente");
        }
    }
}
