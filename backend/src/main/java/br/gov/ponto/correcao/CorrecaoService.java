package br.gov.ponto.correcao;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.common.error.ConflitoException;
import br.gov.ponto.common.error.RecursoNaoEncontradoException;
import br.gov.ponto.common.tempo.TempoMunicipal;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.correcao.api.CorrecaoLoteRequest;
import br.gov.ponto.correcao.domain.CorrecaoMarcacao;
import br.gov.ponto.correcao.domain.StatusCorrecao;
import br.gov.ponto.espelho.CompetenciaService;
import br.gov.ponto.registro.RegistroService;
import br.gov.ponto.registro.domain.RegistroPonto;
import br.gov.ponto.registro.domain.TipoMarcacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Correção de marcação (12.1.4 "esqueci de bater" + 12.6.4 correção do RH em lote).
 * O servidor solicita; a chefia/RH aprova e a marcação é criada (origem AJUSTE, encadeada
 * na cadeia de hash — registros nunca são editados). O RH pode corrigir direto, em lote.
 */
@Service
public class CorrecaoService {

    private final CorrecaoRepository correcaoRepository;
    private final RegistroService registroService;
    private final VinculoRepository vinculoRepository;
    private final CompetenciaService competenciaService;

    public CorrecaoService(CorrecaoRepository correcaoRepository, RegistroService registroService,
                           VinculoRepository vinculoRepository, CompetenciaService competenciaService) {
        this.correcaoRepository = correcaoRepository;
        this.registroService = registroService;
        this.vinculoRepository = vinculoRepository;
        this.competenciaService = competenciaService;
    }

    /** Solicitação de correção (servidor "esqueci de bater" ou RH) — fica PENDENTE. */
    @Transactional
    public CorrecaoMarcacao solicitar(UUID vinculoId, Instant dataHora, TipoMarcacao tipo, String motivo) {
        UUID tenantId = TenantContext.requireCurrent();
        validar(vinculoId, dataHora, tenantId);
        return correcaoRepository.save(new CorrecaoMarcacao(tenantId, vinculoId, dataHora, tipo, motivo));
    }

    @Transactional
    public CorrecaoMarcacao aprovar(UUID id, String motivoDecisao) {
        CorrecaoMarcacao c = exigirPendente(id);
        return aplicarAprovacao(c, motivoDecisao);
    }

    @Transactional
    public CorrecaoMarcacao rejeitar(UUID id, String motivoDecisao) {
        CorrecaoMarcacao c = exigirPendente(id);
        c.rejeitar(motivoDecisao);
        return correcaoRepository.save(c);
    }

    /** Correção direta do RH, em lote (12.6.4): cada item é criado já aprovado, gerando a marcação. */
    @Transactional
    public List<CorrecaoMarcacao> corrigirEmLote(UUID vinculoId, List<CorrecaoLoteRequest.Item> itens, String motivo) {
        UUID tenantId = TenantContext.requireCurrent();
        List<CorrecaoMarcacao> criadas = new ArrayList<>();
        for (CorrecaoLoteRequest.Item item : itens) {
            validar(vinculoId, item.dataHora(), tenantId);
            CorrecaoMarcacao c = new CorrecaoMarcacao(tenantId, vinculoId, item.dataHora(), item.tipo(), motivo);
            criadas.add(aplicarAprovacao(c, "Correção do RH (lote)"));
        }
        return criadas;
    }

    @Transactional(readOnly = true)
    public List<CorrecaoMarcacao> listarPendentes() {
        return correcaoRepository.findByTenantIdAndStatusOrderBySolicitadoEmDesc(
                TenantContext.requireCurrent(), StatusCorrecao.PENDENTE);
    }

    @Transactional(readOnly = true)
    public List<CorrecaoMarcacao> listarPorVinculo(UUID vinculoId) {
        return correcaoRepository.findByVinculoIdAndTenantIdOrderBySolicitadoEmDesc(
                vinculoId, TenantContext.requireCurrent());
    }

    private CorrecaoMarcacao aplicarAprovacao(CorrecaoMarcacao c, String motivoDecisao) {
        RegistroPonto reg = registroService.registrarCorrecao(
                c.getVinculoId(), c.getDataHora(), c.getTipo(), c.getMotivo());
        c.aprovar(motivoDecisao, reg.getId());
        return correcaoRepository.save(c);
    }

    private CorrecaoMarcacao exigirPendente(UUID id) {
        CorrecaoMarcacao c = correcaoRepository.findByIdAndTenantId(id, TenantContext.requireCurrent())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Correção inexistente"));
        if (c.getStatus() != StatusCorrecao.PENDENTE) {
            throw new ConflitoException("Correção já foi decidida");
        }
        return c;
    }

    private void validar(UUID vinculoId, Instant dataHora, UUID tenantId) {
        if (!vinculoRepository.existsByIdAndTenantId(vinculoId, tenantId)) {
            throw new IllegalArgumentException("Vinculo inexistente no ente");
        }
        LocalDate data = dataHora.atZone(TempoMunicipal.ZONE).toLocalDate();
        if (competenciaService.estaFechada(vinculoId, data)) {
            throw new ConflitoException("Competência fechada para a data da correção");
        }
    }
}
