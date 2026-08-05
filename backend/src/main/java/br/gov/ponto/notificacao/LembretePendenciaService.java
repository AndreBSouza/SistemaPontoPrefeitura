package br.gov.ponto.notificacao;

import br.gov.ponto.cadastro.VinculoRepository;
import br.gov.ponto.cadastro.domain.Vinculo;
import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.espelho.CompetenciaRepository;
import br.gov.ponto.espelho.domain.Competencia;
import br.gov.ponto.espelho.domain.StatusCompetencia;
import br.gov.ponto.notificacao.domain.CanalNotificacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Lembretes automáticos de pendência (12.6.7): notifica os servidores cuja competência foi
 * fechada e ainda aguarda ciência. Disparado sob demanda (ou por agendamento na operação).
 */
@Service
public class LembretePendenciaService {

    private static final DateTimeFormatter COMP = DateTimeFormatter.ofPattern("MM/yyyy");

    private final CompetenciaRepository competenciaRepository;
    private final VinculoRepository vinculoRepository;
    private final NotificacaoService notificacaoService;

    public LembretePendenciaService(CompetenciaRepository competenciaRepository,
                                    VinculoRepository vinculoRepository,
                                    NotificacaoService notificacaoService) {
        this.competenciaRepository = competenciaRepository;
        this.vinculoRepository = vinculoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public int lembrarCienciasPendentes() {
        UUID tenantId = TenantContext.requireCurrent();
        int enviados = 0;
        for (Competencia c : competenciaRepository
                .findByTenantIdAndStatusAndCienciaEmIsNull(tenantId, StatusCompetencia.FECHADA)) {
            UUID servidorId = vinculoRepository.findByIdAndTenantId(c.getVinculoId(), tenantId)
                    .map(Vinculo::getServidorId).orElse(null);
            if (servidorId == null) {
                continue;
            }
            notificacaoService.enviar(servidorId.toString(), "Dê ciência do seu espelho",
                    "A competência " + c.getAnoMes().format(COMP)
                            + " está fechada e aguarda a sua ciência.", CanalNotificacao.PUSH);
            enviados++;
        }
        return enviados;
    }
}
