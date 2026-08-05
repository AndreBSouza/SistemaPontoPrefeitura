package br.gov.ponto.me;

import br.gov.ponto.apuracao.JustificativaRepository;
import br.gov.ponto.apuracao.JustificativaService;
import br.gov.ponto.apuracao.api.JustificativaResponse;
import br.gov.ponto.apuracao.domain.Justificativa;
import br.gov.ponto.cadastro.LotacaoRepository;
import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Lotacao;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.error.AcessoNegadoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.delegacao.DelegacaoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * App do gestor (12.3.11): o servidor por trás do dispositivo, quando é chefia de algum órgão
 * (ou substituto por delegação vigente), pode ver e decidir as justificativas do seu time pelo
 * app. A aprovação respeita a alçada do gestor (acima do limite sobe para o RH) e só atua sobre
 * vínculos do próprio time — nunca sobre servidores de fora da sua chefia.
 */
@Service
public class GestorService {

    private final VinculoRepository vinculoRepository;
    private final LotacaoRepository lotacaoRepository;
    private final DelegacaoService delegacaoService;
    private final JustificativaService justificativaService;
    private final JustificativaRepository justificativaRepository;

    public GestorService(VinculoRepository vinculoRepository, LotacaoRepository lotacaoRepository,
                         DelegacaoService delegacaoService, JustificativaService justificativaService,
                         JustificativaRepository justificativaRepository) {
        this.vinculoRepository = vinculoRepository;
        this.lotacaoRepository = lotacaoRepository;
        this.delegacaoService = delegacaoService;
        this.justificativaService = justificativaService;
        this.justificativaRepository = justificativaRepository;
    }

    /** O servidor do dispositivo chefia algum órgão (ou é substituto por delegação)? */
    @Transactional(readOnly = true)
    public boolean souGestor(UUID vinculoId) {
        return !lotacoesDoGestor(vinculoId).isEmpty();
    }

    /** Justificativas pendentes do time do gestor (inclui chefias delegadas). */
    @Transactional(readOnly = true)
    public List<JustificativaResponse> pendentesDoMeuTime(UUID vinculoId) {
        return justificativaService.pendentesDaChefia(servidorDoVinculo(vinculoId));
    }

    /** Aprova uma justificativa do time (com alçada de gestor — acima do limite sobe para o RH). */
    public JustificativaResponse aprovar(UUID vinculoIdGestor, UUID justificativaId, String motivo) {
        exigirJustificativaDoTime(vinculoIdGestor, justificativaId);
        return justificativaService.aprovarComAlcada(justificativaId, motivo, false);
    }

    public JustificativaResponse rejeitar(UUID vinculoIdGestor, UUID justificativaId, String motivo) {
        exigirJustificativaDoTime(vinculoIdGestor, justificativaId);
        return justificativaService.rejeitar(justificativaId, motivo);
    }

    /** Garante que a justificativa pertence a um vínculo do time do gestor (senão 403). */
    private void exigirJustificativaDoTime(UUID vinculoIdGestor, UUID justificativaId) {
        UUID tenantId = TenantContext.requireCurrent();
        Justificativa j = justificativaRepository.findByIdAndTenantId(justificativaId, tenantId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Justificativa inexistente"));
        if (!vinculosDoTime(vinculoIdGestor).contains(j.getVinculoId())) {
            throw new AcessoNegadoException("Justificativa não pertence ao seu time");
        }
    }

    private Set<UUID> lotacoesDoGestor(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        UUID servidorId = servidorDoVinculo(vinculoId);
        Set<UUID> chefias = new HashSet<>(delegacaoService.delegantesAtivosPara(servidorId));
        chefias.add(servidorId);
        return lotacaoRepository.findByTenantIdAndChefiaServidorIdIn(tenantId, chefias)
                .stream().map(Lotacao::getId).collect(Collectors.toSet());
    }

    private Set<UUID> vinculosDoTime(UUID vinculoId) {
        Set<UUID> lotacoes = lotacoesDoGestor(vinculoId);
        if (lotacoes.isEmpty()) {
            return Set.of();
        }
        return vinculoRepository.findByLotacaoIdInAndTenantId(lotacoes, TenantContext.requireCurrent())
                .stream().map(Vinculo::getId).collect(Collectors.toSet());
    }

    private UUID servidorDoVinculo(UUID vinculoId) {
        return vinculoRepository.findByIdAndTenantId(vinculoId, TenantContext.requireCurrent())
                .map(Vinculo::getServidorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo inexistente"));
    }
}
