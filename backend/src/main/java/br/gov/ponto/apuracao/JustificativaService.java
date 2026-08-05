package br.gov.ponto.apuracao;

import br.gov.ponto.apuracao.api.JustificativaResponse;
import br.gov.ponto.apuracao.api.SolicitarJustificativaRequest;
import br.gov.ponto.apuracao.domain.Justificativa;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.auditoria.AuditoriaService;
import br.gov.ponto.cadastro.LotacaoRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.delegacao.DelegacaoService;
import br.gov.ponto.espelho.CompetenciaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JustificativaService {

    /** Alçada padrão do gestor (12.6.13): aprovações acima deste nº de dias sobem para RH/controle. */
    public static final int ALCADA_GESTOR_DIAS = 5;

    private final JustificativaRepository justificativaRepository;
    private final VinculoRepository vinculoRepository;
    private final LotacaoRepository lotacaoRepository;
    private final CompetenciaService competenciaService;
    private final AuditoriaService auditoriaService;
    private final DelegacaoService delegacaoService;

    public JustificativaService(JustificativaRepository justificativaRepository,
                                VinculoRepository vinculoRepository,
                                LotacaoRepository lotacaoRepository,
                                CompetenciaService competenciaService,
                                AuditoriaService auditoriaService,
                                DelegacaoService delegacaoService) {
        this.justificativaRepository = justificativaRepository;
        this.vinculoRepository = vinculoRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.competenciaService = competenciaService;
        this.auditoriaService = auditoriaService;
        this.delegacaoService = delegacaoService;
    }

    @Transactional
    public JustificativaResponse solicitar(SolicitarJustificativaRequest request) {
        UUID tenantId = TenantContext.requireCurrent();
        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new IllegalArgumentException("dataFim nao pode ser anterior a dataInicio");
        }
        if (!vinculoRepository.existsByIdAndTenantId(request.vinculoId(), tenantId)) {
            throw new IllegalArgumentException("Vinculo inexistente no ente");
        }
        if (competenciaService.estaFechada(request.vinculoId(), request.dataInicio())) {
            throw new ConflitoException("Competencia fechada para o periodo da justificativa");
        }
        Justificativa j = justificativaRepository.save(new Justificativa(
                tenantId, request.vinculoId(), request.tipo(),
                request.dataInicio(), request.dataFim(), request.motivo(), request.anexo()));
        return JustificativaResponse.from(j);
    }

    @Transactional
    public JustificativaResponse aprovar(UUID id, String motivoDecisao) {
        return decidir(id, StatusJustificativa.APROVADA, motivoDecisao);
    }

    /**
     * Aprovação com alçada / segregação de funções (12.6.13): um gestor só aprova ausências de até
     * {@link #ALCADA_GESTOR_DIAS} dias; acima disso, a decisão sobe para o RH/controle.
     * {@code aprovadorEhRh} (RH / controladoria / admin) aprova sem limite.
     */
    @Transactional
    public JustificativaResponse aprovarComAlcada(UUID id, String motivoDecisao, boolean aprovadorEhRh) {
        if (!aprovadorEhRh) {
            Justificativa j = justificativaRepository.findByIdAndTenantId(id, TenantContext.requireCurrent())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Justificativa inexistente"));
            long dias = ChronoUnit.DAYS.between(j.getDataInicio(), j.getDataFim()) + 1;
            if (dias > ALCADA_GESTOR_DIAS) {
                throw new ConflitoException("Acima da alçada do gestor (" + ALCADA_GESTOR_DIAS
                        + " dias): requer aprovação do RH/controle");
            }
        }
        return decidir(id, StatusJustificativa.APROVADA, motivoDecisao);
    }

    @Transactional
    public JustificativaResponse rejeitar(UUID id, String motivoDecisao) {
        return decidir(id, StatusJustificativa.REJEITADA, motivoDecisao);
    }

    /** Aprovação em lote (caixa de aprovações): ignora inexistentes/já decididas. */
    @Transactional
    public List<JustificativaResponse> aprovarEmLote(List<UUID> ids, String motivoDecisao) {
        return decidirEmLote(ids, StatusJustificativa.APROVADA, motivoDecisao);
    }

    /** Recusa em lote (caixa de aprovações): ignora inexistentes/já decididas. */
    @Transactional
    public List<JustificativaResponse> rejeitarEmLote(List<UUID> ids, String motivoDecisao) {
        return decidirEmLote(ids, StatusJustificativa.REJEITADA, motivoDecisao);
    }

    private List<JustificativaResponse> decidirEmLote(List<UUID> ids, StatusJustificativa status,
                                                      String motivoDecisao) {
        UUID tenantId = TenantContext.requireCurrent();
        List<JustificativaResponse> decididas = new ArrayList<>();
        for (UUID id : ids) {
            Justificativa j = justificativaRepository.findByIdAndTenantId(id, tenantId).orElse(null);
            // Lote robusto/idempotente: pula o que nao existe ou ja foi decidido.
            if (j == null || j.getStatus() != StatusJustificativa.PENDENTE) {
                continue;
            }
            j.decidir(status, null, motivoDecisao);
            Justificativa salva = justificativaRepository.save(j);
            auditoriaService.registrar("DECISAO_JUSTIFICATIVA", "justificativa", salva.getId().toString(),
                    status.name() + " (lote)" + (motivoDecisao != null ? ": " + motivoDecisao : ""));
            decididas.add(JustificativaResponse.from(salva));
        }
        return decididas;
    }

    private JustificativaResponse decidir(UUID id, StatusJustificativa status, String motivoDecisao) {
        UUID tenantId = TenantContext.requireCurrent();
        Justificativa j = justificativaRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Justificativa inexistente"));
        if (j.getStatus() != StatusJustificativa.PENDENTE) {
            throw new ConflitoException("Justificativa ja foi decidida");
        }
        // aprovadorId virá do usuario autenticado (sub do JWT) quando o login estiver mapeado.
        j.decidir(status, null, motivoDecisao);
        Justificativa salva = justificativaRepository.save(j);
        auditoriaService.registrar("DECISAO_JUSTIFICATIVA", "justificativa", salva.getId().toString(),
                status.name() + (motivoDecisao != null ? ": " + motivoDecisao : ""));
        return JustificativaResponse.from(salva);
    }

    @Transactional(readOnly = true)
    public List<JustificativaResponse> listarPendentes() {
        UUID tenantId = TenantContext.requireCurrent();
        return justificativaRepository.findByTenantIdAndStatus(tenantId, StatusJustificativa.PENDENTE)
                .stream().map(JustificativaResponse::from).toList();
    }

    /**
     * Justificativas pendentes dos servidores lotados sob a chefia informada (hierarquia por
     * lotacao). Inclui também as chefias dos gestores que delegaram a aprovação a este servidor
     * no período vigente (12.6.8 — substituto nas férias do gestor).
     */
    @Transactional(readOnly = true)
    public List<JustificativaResponse> pendentesDaChefia(UUID chefiaServidorId) {
        UUID tenantId = TenantContext.requireCurrent();
        Set<UUID> chefias = new HashSet<>(delegacaoService.delegantesAtivosPara(chefiaServidorId));
        chefias.add(chefiaServidorId);
        List<UUID> lotacoes = lotacaoRepository
                .findByTenantIdAndChefiaServidorIdIn(tenantId, chefias)
                .stream().map(Lotacao::getId).toList();
        if (lotacoes.isEmpty()) {
            return List.of();
        }
        List<UUID> vinculos = vinculoRepository.findByLotacaoIdInAndTenantId(lotacoes, tenantId)
                .stream().map(Vinculo::getId).toList();
        if (vinculos.isEmpty()) {
            return List.of();
        }
        return justificativaRepository
                .findByVinculoIdInAndTenantIdAndStatus(vinculos, tenantId, StatusJustificativa.PENDENTE)
                .stream().map(JustificativaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<JustificativaResponse> listarPorVinculo(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        return justificativaRepository.findByVinculoIdAndTenantId(vinculoId, tenantId)
                .stream().map(JustificativaResponse::from).toList();
    }

}
