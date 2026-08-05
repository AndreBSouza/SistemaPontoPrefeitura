package br.gov.ponto.saas.identidade;

import java.util.UUID;

/**
 * Provisiona o 1º administrador do ente no provedor de identidade (Keycloak) quando a adesão é
 * aprovada — cria o usuário {@code tenant-admin} com o atributo {@code tenant_id} = UUID do ente
 * (o claim que o backend usa para isolar os dados). Sem isso, ninguém consegue logar no ente novo.
 *
 * <p>Seam: o default {@link ProvisionadorIdentidadeManual} apenas registra um runbook (para criação
 * manual); a implementação real {@link KeycloakAdminProvisionador} (bean {@code @Primary}, ativada
 * por {@code keycloak.admin.server-url}) cria o usuário via API de admin do Keycloak.</p>
 */
public interface ProvisionadorIdentidade {

    /**
     * Cria (ou garante) o usuário administrador do ente no IdP.
     *
     * @return credencial temporária gerada, se aplicável (para comunicar ao responsável), ou vazio.
     */
    java.util.Optional<String> provisionarAdmin(UUID tenantId, String slug, String nomeEnte,
                                                String emailResponsavel, String nomeResponsavel);
}
