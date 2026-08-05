package br.gov.ponto.saas.identidade;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prova o fluxo de provisionamento contra a API de admin do Keycloak SEM um Keycloak real
 * (MockRestServiceServer): token → cria usuário com tenant_id → atribui role → dispara e-mail.
 */
class KeycloakAdminProvisionadorTest {

    @Test
    void provisionaAdminComTenantIdERoleTenantAdmin() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://kc");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();
        KeycloakAdminProvisionador prov =
                new KeycloakAdminProvisionador(rest, "ponto", "master", "ponto-provisioner", "segredo");

        UUID tenantId = UUID.randomUUID();

        server.expect(requestTo("http://kc/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok-123\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://kc/admin/realms/ponto/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok-123"))
                .andExpect(jsonPath("$.email").value("admin@cidade.gov.br"))
                .andExpect(jsonPath("$.attributes.tenant_id[0]").value(tenantId.toString()))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .header("Location", "http://kc/admin/realms/ponto/users/USER-1"));

        server.expect(requestTo("http://kc/admin/realms/ponto/roles/tenant-admin"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"r1\",\"name\":\"tenant-admin\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://kc/admin/realms/ponto/users/USER-1/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].name").value("tenant-admin"))
                .andRespond(withSuccess());

        server.expect(requestTo("http://kc/admin/realms/ponto/users/USER-1/execute-actions-email"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess());

        Optional<String> r = prov.provisionarAdmin(
                tenantId, "cidade", "Prefeitura de Cidade", "admin@cidade.gov.br", "Fulano");

        server.verify();
        assertThat(r).isEmpty();
    }
}
