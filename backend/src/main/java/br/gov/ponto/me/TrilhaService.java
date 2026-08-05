package br.gov.ponto.me;

import br.gov.ponto.apuracao.JustificativaRepository;
import br.gov.ponto.apuracao.domain.Justificativa;
import br.gov.ponto.apuracao.domain.StatusJustificativa;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.correcao.CorrecaoService;
import br.gov.ponto.correcao.domain.CorrecaoMarcacao;
import br.gov.ponto.correcao.domain.StatusCorrecao;
import br.gov.ponto.me.api.TrilhaEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Trilha pessoal do servidor (12.1.2): monta, em ordem cronológica decrescente, o histórico do
 * que aconteceu com os registros do vínculo — correções de marcação e justificativas/atestados,
 * incluindo a decisão (aprovada/rejeitada) e o motivo. Transparência para o próprio servidor.
 */
@Service
public class TrilhaService {

    private final CorrecaoService correcaoService;
    private final JustificativaRepository justificativaRepository;

    public TrilhaService(CorrecaoService correcaoService, JustificativaRepository justificativaRepository) {
        this.correcaoService = correcaoService;
        this.justificativaRepository = justificativaRepository;
    }

    @Transactional(readOnly = true)
    public List<TrilhaEvento> montar(UUID vinculoId) {
        UUID tenantId = TenantContext.requireCurrent();
        List<TrilhaEvento> eventos = new ArrayList<>();

        for (CorrecaoMarcacao c : correcaoService.listarPorVinculo(vinculoId)) {
            eventos.add(new TrilhaEvento(c.getSolicitadoEm(), "CORRECAO",
                    "Correção de marcação solicitada",
                    c.getTipo() + " em " + c.getDataHora()
                            + (c.getMotivo() != null ? " — " + c.getMotivo() : "")));
            if (c.getStatus() != StatusCorrecao.PENDENTE && c.getDecididoEm() != null) {
                eventos.add(new TrilhaEvento(c.getDecididoEm(), "CORRECAO",
                        "Correção " + rotulo(c.getStatus().name()),
                        c.getMotivoDecisao() != null ? c.getMotivoDecisao() : ""));
            }
        }

        for (Justificativa j : justificativaRepository.findByVinculoIdAndTenantId(vinculoId, tenantId)) {
            eventos.add(new TrilhaEvento(j.getCriadoEm(), "JUSTIFICATIVA",
                    j.getTipo() + " solicitada",
                    j.getDataInicio() + " a " + j.getDataFim()
                            + (j.getMotivo() != null ? " — " + j.getMotivo() : "")));
            if (j.getStatus() != StatusJustificativa.PENDENTE && j.getDecisaoEm() != null) {
                eventos.add(new TrilhaEvento(j.getDecisaoEm(), "JUSTIFICATIVA",
                        j.getTipo() + " " + rotulo(j.getStatus().name()),
                        j.getMotivoDecisao() != null ? j.getMotivoDecisao() : ""));
            }
        }

        eventos.sort(Comparator.comparing(TrilhaEvento::instante).reversed());
        return eventos;
    }

    private String rotulo(String status) {
        return switch (status) {
            case "APROVADA" -> "aprovada";
            case "REJEITADA" -> "rejeitada";
            default -> "atualizada";
        };
    }
}
