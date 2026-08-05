package br.gov.ponto.notificacao;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.notificacao.domain.CanalNotificacao;
import br.gov.ponto.notificacao.domain.Notificacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final Notificador notificador;

    public NotificacaoService(NotificacaoRepository notificacaoRepository, Notificador notificador) {
        this.notificacaoRepository = notificacaoRepository;
        this.notificador = notificador;
    }

    @Transactional
    public Notificacao enviar(String destinatario, String assunto, String mensagem, CanalNotificacao canal) {
        UUID tenantId = TenantContext.requireCurrent();
        Notificacao n = notificacaoRepository.save(
                new Notificacao(tenantId, destinatario, assunto, mensagem, canal));
        notificador.enviar(n);
        return n;
    }

    @Transactional(readOnly = true)
    public List<Notificacao> listar(String destinatario) {
        return notificacaoRepository.findByTenantIdAndDestinatarioOrderByEnviadaEmDesc(
                TenantContext.requireCurrent(), destinatario);
    }
}
