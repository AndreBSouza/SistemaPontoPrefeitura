package br.gov.ponto.saas.identidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Default (dev / sem Keycloak admin configurado): NÃO cria usuário; apenas registra um runbook para
 * o operador criar o {@code tenant-admin} manualmente no Keycloak com o atributo {@code tenant_id}.
 * Nunca lança — a aprovação da adesão (tenant já criado) não pode falhar por causa do IdP.
 */
@Service
public class ProvisionadorIdentidadeManual implements ProvisionadorIdentidade {

    private static final Logger log = LoggerFactory.getLogger(ProvisionadorIdentidadeManual.class);

    @Override
    public Optional<String> provisionarAdmin(UUID tenantId, String slug, String nomeEnte,
                                             String emailResponsavel, String nomeResponsavel) {
        log.warn("""
                [PROVISIONAMENTO MANUAL] Crie o administrador do ente no Keycloak:
                  realm: ponto | usuário: {} | e-mail: {}
                  atributo OBRIGATÓRIO tenant_id = {}
                  role de realm: tenant-admin
                (configure keycloak.admin.server-url para automatizar este passo.)""",
                emailResponsavel, emailResponsavel, tenantId);
        return Optional.empty();
    }
}
