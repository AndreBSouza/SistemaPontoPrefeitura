package br.gov.ponto.comunicado;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.comunicado.domain.Comunicado;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Comunicados oficiais (broadcast) — canal direto prefeitura<->servidores (12.3.7).
 * A prefeitura (RH/tenant-admin) publica; o servidor lê no app os comunicados gerais
 * mais os do seu órgão.
 */
@Service
public class ComunicadoService {

    private final ComunicadoRepository comunicadoRepository;
    private final VinculoRepository vinculoRepository;

    public ComunicadoService(ComunicadoRepository comunicadoRepository, VinculoRepository vinculoRepository) {
        this.comunicadoRepository = comunicadoRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional
    public Comunicado publicar(String titulo, String mensagem, UUID lotacaoId) {
        UUID tenantId = TenantContext.requireCurrent();
        return comunicadoRepository.save(new Comunicado(tenantId, titulo, mensagem, lotacaoId));
    }

    /** Todos os comunicados do ente (visão do painel administrativo). */
    @Transactional(readOnly = true)
    public List<Comunicado> listarTodos() {
        return comunicadoRepository.findByTenantIdOrderByPublicadoEmDesc(TenantContext.requireCurrent());
    }

    /** Comunicados visíveis ao vínculo: gerais (sem órgão) + os do órgão em que está lotado. */
    @Transactional(readOnly = true)
    public List<Comunicado> listarParaVinculo(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        Vinculo vinculo = vinculoRepository.findByIdAndTenantId(vinculoId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo inexistente"));
        UUID lotacaoId = vinculo.getLotacaoId();
        return comunicadoRepository.findByTenantIdOrderByPublicadoEmDesc(tenantId).stream()
                .filter(c -> c.isGeral() || (lotacaoId != null && lotacaoId.equals(c.getLotacaoId())))
                .toList();
    }
}
