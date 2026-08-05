package br.gov.ponto.saas;

import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.saas.api.OnboardingRequest;
import br.gov.ponto.saas.api.OnboardingResponse;
import br.gov.ponto.saas.api.SolicitacaoEnteResponse;
import br.gov.ponto.saas.api.SolicitarEnteRequest;
import br.gov.ponto.saas.domain.SolicitacaoEnte;
import br.gov.ponto.saas.domain.StatusSolicitacao;
import br.gov.ponto.saas.identidade.ProvisionadorIdentidade;
import br.gov.ponto.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Onboarding self-service do ente (12.3.13): o município solicita adesão por um endpoint público;
 * a solicitação fica PENDENTE e só é provisionada (vira tenant) após aprovação do operador da
 * plataforma. Fluxo "pedido → aprovação manual → provisionamento" para evitar criação indevida.
 */
@Service
public class SolicitacaoEnteService {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoEnteService.class);

    private final SolicitacaoEnteRepository repository;
    private final OnboardingService onboardingService;
    private final TenantRepository tenantRepository;
    private final ProvisionadorIdentidade provisionadorIdentidade;

    public SolicitacaoEnteService(SolicitacaoEnteRepository repository, OnboardingService onboardingService,
                                  TenantRepository tenantRepository,
                                  ProvisionadorIdentidade provisionadorIdentidade) {
        this.repository = repository;
        this.onboardingService = onboardingService;
        this.tenantRepository = tenantRepository;
        this.provisionadorIdentidade = provisionadorIdentidade;
    }

    /** Solicitação pública (não cria tenant — apenas registra o pedido para aprovação). */
    @Transactional
    public SolicitacaoEnteResponse solicitar(SolicitarEnteRequest request) {
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new ConflitoException("O identificador \"" + request.slug() + "\" já está em uso por um ente.");
        }
        if (repository.existsBySlugAndStatus(request.slug(), StatusSolicitacao.PENDENTE)) {
            throw new ConflitoException("Já existe uma solicitação pendente para o identificador \"" + request.slug() + "\"");
        }
        SolicitacaoEnte solicitacao = repository.save(new SolicitacaoEnte(
                request.nome(), request.slug(), request.tipoPoder(),
                request.responsavelNome(), request.responsavelEmail()));
        return SolicitacaoEnteResponse.from(solicitacao);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoEnteResponse> listarPendentes() {
        return repository.findByStatusOrderByCriadoEm(StatusSolicitacao.PENDENTE)
                .stream().map(SolicitacaoEnteResponse::from).toList();
    }

    /**
     * Aprovação pelo operador: provisiona o tenant e marca a solicitação como aprovada.
     * NÃO é @Transactional de propósito — {@link OnboardingService#provisionar} cria o tenant e a
     * lotação inicial em transações próprias, alternando o {@code TenantContext} (RLS); envolvê-lo
     * numa transação externa fixaria a conexão/GUC e quebraria a inserção da lotação.
     */
    public OnboardingResponse aprovar(UUID id) {
        SolicitacaoEnte solicitacao = exigirPendente(id);
        OnboardingResponse provisionado = onboardingService.provisionar(new OnboardingRequest(
                solicitacao.getNome(), solicitacao.getSlug(), solicitacao.getTipoPoder(), "Gabinete"));
        solicitacao.aprovar(provisionado.tenantId());
        repository.save(solicitacao);
        // Cria o 1º admin do ente no IdP (Keycloak). Best-effort: o tenant já existe, então uma falha
        // no IdP não invalida a aprovação — cai no runbook manual (ver ProvisionadorIdentidadeManual).
        try {
            provisionadorIdentidade.provisionarAdmin(provisionado.tenantId(), solicitacao.getSlug(),
                    solicitacao.getNome(), solicitacao.getResponsavelEmail(), solicitacao.getResponsavelNome());
        } catch (RuntimeException e) {
            log.warn("Falha ao provisionar o admin do ente {} no IdP ({}); criar manualmente.",
                    solicitacao.getSlug(), e.getClass().getSimpleName());
        }
        return provisionado;
    }

    @Transactional
    public void rejeitar(UUID id, String motivo) {
        SolicitacaoEnte solicitacao = exigirPendente(id);
        solicitacao.rejeitar(motivo);
        repository.save(solicitacao);
    }

    private SolicitacaoEnte exigirPendente(UUID id) {
        SolicitacaoEnte solicitacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação inexistente"));
        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new ConflitoException("Solicitação já foi decidida");
        }
        return solicitacao;
    }
}
