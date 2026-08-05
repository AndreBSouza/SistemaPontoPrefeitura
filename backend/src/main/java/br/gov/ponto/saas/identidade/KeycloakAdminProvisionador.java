package br.gov.ponto.saas.identidade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Provisiona o administrador do ente via API de administração do Keycloak (bean {@code @Primary},
 * ativado por {@code keycloak.admin.server-url}). Sem novas dependências — usa o {@link RestClient}.
 *
 * <p>Passos: (1) obtém token de admin por {@code client_credentials} (service account com role
 * {@code manage-users}); (2) cria o usuário com o atributo {@code tenant_id} = UUID do ente e a
 * required action UPDATE_PASSWORD; (3) atribui a role de realm {@code tenant-admin}; (4) dispara o
 * e-mail para o usuário definir a senha (best-effort). Config: {@code keycloak.admin.server-url},
 * {@code .realm} (default ponto), {@code .token-realm} (default = realm), {@code .client-id},
 * {@code .client-secret}.</p>
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "keycloak.admin", name = "server-url")
public class KeycloakAdminProvisionador implements ProvisionadorIdentidade {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminProvisionador.class);

    private final RestClient rest;
    private final String realm;
    private final String tokenRealm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminProvisionador(
            @Value("${keycloak.admin.server-url}") String serverUrl,
            @Value("${keycloak.admin.realm:ponto}") String realm,
            @Value("${keycloak.admin.token-realm:}") String tokenRealm,
            @Value("${keycloak.admin.client-id:ponto-provisioner}") String clientId,
            @Value("${keycloak.admin.client-secret:}") String clientSecret) {
        this(RestClient.builder().baseUrl(semBarraFinal(serverUrl)).build(),
                realm, (tokenRealm == null || tokenRealm.isBlank()) ? realm : tokenRealm, clientId, clientSecret);
    }

    KeycloakAdminProvisionador(RestClient rest, String realm, String tokenRealm,
                               String clientId, String clientSecret) {
        this.rest = rest;
        this.realm = realm;
        this.tokenRealm = tokenRealm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public Optional<String> provisionarAdmin(UUID tenantId, String slug, String nomeEnte,
                                             String emailResponsavel, String nomeResponsavel) {
        String token = obterToken();
        String userId = criarUsuario(token, tenantId, emailResponsavel, nomeResponsavel);
        atribuirRoleTenantAdmin(token, userId);
        enviarEmailDefinirSenha(token, userId);
        log.info("Administrador do ente {} provisionado no Keycloak (userId={}).", slug, userId);
        return Optional.empty();
    }

    private String obterToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        Map<?, ?> resp = rest.post()
                .uri("/realms/{r}/protocol/openid-connect/token", tokenRealm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(Map.class);
        Object at = resp == null ? null : resp.get("access_token");
        if (at == null) {
            throw new IllegalStateException("Keycloak não retornou access_token para o provisionamento");
        }
        return at.toString();
    }

    private String criarUsuario(String token, UUID tenantId, String email, String nome) {
        Map<String, Object> corpo = Map.of(
                "username", email,
                "email", email,
                "firstName", nome == null ? "" : nome,
                "enabled", true,
                "emailVerified", false,
                "attributes", Map.of("tenant_id", List.of(tenantId.toString())),
                "requiredActions", List.of("UPDATE_PASSWORD"));
        ResponseEntity<Void> resp = rest.post()
                .uri("/admin/realms/{r}/users", realm)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve().toBodilessEntity();
        String location = resp.getHeaders().getFirst("Location");
        if (location == null || !location.contains("/")) {
            throw new IllegalStateException("Keycloak não retornou o Location do usuário criado");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private void atribuirRoleTenantAdmin(String token, String userId) {
        Map<?, ?> role = rest.get()
                .uri("/admin/realms/{r}/roles/{n}", realm, "tenant-admin")
                .header("Authorization", "Bearer " + token)
                .retrieve().body(Map.class);
        rest.post()
                .uri("/admin/realms/{r}/users/{u}/role-mappings/realm", realm, userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve().toBodilessEntity();
    }

    private void enviarEmailDefinirSenha(String token, String userId) {
        try {
            rest.put()
                    .uri("/admin/realms/{r}/users/{u}/execute-actions-email", realm, userId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of("UPDATE_PASSWORD"))
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            // SMTP do Keycloak pode não estar configurado — o usuário já existe; senão, reset manual.
            log.warn("Usuário criado, mas o e-mail para definir senha falhou ({}). Envie um reset manual.",
                    e.getClass().getSimpleName());
        }
    }

    private static String semBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
