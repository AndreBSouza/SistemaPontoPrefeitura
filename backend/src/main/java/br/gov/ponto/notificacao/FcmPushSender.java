package br.gov.ponto.notificacao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Envio de push pelo <b>Firebase Cloud Messaging (API HTTP v1)</b>.
 *
 * <p>Ativado por {@code fcm.credenciais} (caminho do JSON da conta de serviço do projeto Firebase).
 * Sem isso, prevalece o registro em log — o gating de notificação não muda.</p>
 *
 * <p>Fala direto com a API REST oficial: monta e assina um JWT (RS256) com a chave da conta de
 * serviço, troca por um access token OAuth2 e envia a mensagem. Sem o SDK {@code firebase-admin},
 * que traria gRPC/protobuf/OpenTelemetry para o que são duas chamadas HTTP.</p>
 *
 * <p>O {@code destinatario} da notificação deve ser o <b>registration token</b> do aparelho
 * (obtido pelo app no Firebase e registrado no backend).</p>
 */
@Component
@ConditionalOnProperty(prefix = "fcm", name = "credenciais")
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);
    private static final String ESCOPO = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String AUD_TOKEN = "https://oauth2.googleapis.com/token";
    private static final long VALIDADE_SEGUNDOS = 3600;

    private final RestClient rest;
    private final ObjectMapper mapper;
    private final String projectId;
    private final String clientEmail;
    private final PrivateKey chavePrivada;
    private final String urlToken;
    private final String urlFcm;

    /** Access token OAuth2 em cache até pouco antes de expirar (evita 1 troca por push). */
    private volatile String tokenCache;
    private volatile Instant tokenExpiraEm = Instant.EPOCH;

    public FcmPushSender(@Value("${fcm.credenciais}") String caminhoCredenciais,
                         @Value("${fcm.url-token:" + AUD_TOKEN + "}") String urlToken,
                         @Value("${fcm.url-base:https://fcm.googleapis.com}") String urlBase,
                         ObjectMapper mapper) {
        this(RestClient.builder().build(), lerCredenciais(caminhoCredenciais, mapper), urlToken, urlBase, mapper);
    }

    FcmPushSender(RestClient rest, Credenciais credenciais, String urlToken, String urlBase, ObjectMapper mapper) {
        this.rest = rest;
        this.mapper = mapper;
        this.projectId = credenciais.projectId();
        this.clientEmail = credenciais.clientEmail();
        this.chavePrivada = credenciais.chavePrivada();
        this.urlToken = urlToken;
        this.urlFcm = urlBase + "/v1/projects/" + credenciais.projectId() + "/messages:send";
        log.info("Push FCM habilitado (projeto {}).", projectId);
    }

    /** Dados extraídos do JSON da conta de serviço. */
    record Credenciais(String projectId, String clientEmail, PrivateKey chavePrivada) {
    }

    static Credenciais lerCredenciais(String caminho, ObjectMapper mapper) {
        try {
            JsonNode json = mapper.readTree(Files.readString(Path.of(caminho), StandardCharsets.UTF_8));
            String pem = json.path("private_key").asText();
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            PrivateKey chave = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
            return new Credenciais(json.path("project_id").asText(),
                    json.path("client_email").asText(), chave);
        } catch (IOException | RuntimeException | java.security.GeneralSecurityException e) {
            // Credencial inválida deve derrubar o startup — melhor do que "push silenciosamente morto".
            throw new IllegalStateException("Credenciais do FCM inválidas (fcm.credenciais): "
                    + e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void enviar(String destinatario, String titulo, String mensagem) {
        Map<String, Object> corpo = Map.of("message", Map.of(
                "token", destinatario,
                "notification", Map.of(
                        "title", titulo == null ? "" : titulo,
                        "body", mensagem == null ? "" : mensagem)));
        rest.post()
                .uri(urlFcm)
                .header("Authorization", "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();
    }

    /** Access token OAuth2 (renovado 5 min antes de expirar). */
    private String accessToken() {
        if (tokenCache != null && Instant.now().isBefore(tokenExpiraEm)) {
            return tokenCache;
        }
        synchronized (this) {
            if (tokenCache != null && Instant.now().isBefore(tokenExpiraEm)) {
                return tokenCache;
            }
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
            form.add("assertion", montarJwt());
            Map<?, ?> resp = rest.post()
                    .uri(urlToken)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve().body(Map.class);
            Object at = resp == null ? null : resp.get("access_token");
            if (at == null) {
                throw new IllegalStateException("FCM não retornou access_token");
            }
            tokenCache = at.toString();
            tokenExpiraEm = Instant.now().plusSeconds(VALIDADE_SEGUNDOS - 300);
            return tokenCache;
        }
    }

    /** JWT assinado (RS256) da conta de serviço, trocado por access token no OAuth2 do Google. */
    String montarJwt() {
        long agora = Instant.now().getEpochSecond();
        String cabecalho = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String claims = base64Url(String.format(
                "{\"iss\":\"%s\",\"scope\":\"%s\",\"aud\":\"%s\",\"iat\":%d,\"exp\":%d}",
                clientEmail, ESCOPO, AUD_TOKEN, agora, agora + VALIDADE_SEGUNDOS));
        String conteudo = cabecalho + "." + claims;
        try {
            Signature assinatura = Signature.getInstance("SHA256withRSA");
            assinatura.initSign(chavePrivada);
            assinatura.update(conteudo.getBytes(StandardCharsets.UTF_8));
            String assinado = Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura.sign());
            return conteudo + "." + assinado;
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao assinar o JWT do FCM", e);
        }
    }

    private static String base64Url(String texto) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
