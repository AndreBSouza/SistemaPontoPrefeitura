package br.gov.ponto.delegacao;

import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.delegacao.domain.Delegacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Delegação de aprovação (12.6.8): um gestor titular delega, por um período, as suas
 * pendências de chefia a um substituto (ex.: durante as férias). Consultada pelos fluxos
 * de aprovação (justificativas) para expandir o conjunto de chefias do substituto.
 */
@Service
public class DelegacaoService {

    private final DelegacaoRepository delegacaoRepository;

    public DelegacaoService(DelegacaoRepository delegacaoRepository) {
        this.delegacaoRepository = delegacaoRepository;
    }

    @Transactional
    public Delegacao criar(UUID deleganteServidorId, UUID delegadoServidorId,
                           LocalDate dataInicio, LocalDate dataFim) {
        UUID tenantId = TenantContext.requireCurrent();
        if (dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("dataFim nao pode ser anterior a dataInicio");
        }
        if (deleganteServidorId.equals(delegadoServidorId)) {
            throw new IllegalArgumentException("Delegante e delegado nao podem ser o mesmo servidor");
        }
        return delegacaoRepository.save(
                new Delegacao(tenantId, deleganteServidorId, delegadoServidorId, dataInicio, dataFim));
    }

    @Transactional
    public void revogar(UUID id) {
        UUID tenantId = TenantContext.requireCurrent();
        Delegacao d = delegacaoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Delegação inexistente"));
        d.revogar();
        delegacaoRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<Delegacao> listar() {
        return delegacaoRepository.findByTenantIdOrderByCriadoEmDesc(TenantContext.requireCurrent());
    }

    /** Servidores (gestores) que delegaram a aprovação ao {@code delegadoServidorId} hoje. */
    @Transactional(readOnly = true)
    public List<UUID> delegantesAtivosPara(UUID delegadoServidorId) {
        LocalDate hoje = LocalDate.now(TempoMunicipal.ZONE);
        return delegacaoRepository
                .findByTenantIdAndDelegadoServidorIdAndAtivoTrueAndDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        TenantContext.requireCurrent(), delegadoServidorId, hoje, hoje)
                .stream().map(Delegacao::getDeleganteServidorId).toList();
    }
}
