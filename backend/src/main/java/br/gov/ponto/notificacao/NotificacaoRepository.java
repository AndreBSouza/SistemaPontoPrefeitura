package br.gov.ponto.notificacao;

import br.gov.ponto.notificacao.domain.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    List<Notificacao> findByTenantIdAndDestinatarioOrderByEnviadaEmDesc(UUID tenantId, String destinatario);
}
