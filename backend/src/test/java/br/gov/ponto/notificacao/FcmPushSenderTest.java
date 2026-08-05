package br.gov.ponto.notificacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Exercita o envio real de push (API HTTP v1 do FCM) sem rede: troca do JWT por access token,
 * cache do token e formato da mensagem.
 */
class FcmPushSenderTest {

    private static KeyPair par() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

    private FcmPushSender montar(RestClient rest, KeyPair kp) {
        return new FcmPushSender(rest,
                new FcmPushSender.Credenciais("proj-1", "robo@proj-1.iam.gserviceaccount.com", kp.getPrivate()),
                "https://oauth2.test/token", "https://fcm.test", new ObjectMapper());
    }

    @Test
    void trocaJwtPorTokenEEnviaMensagemComOTokenDoAparelho() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FcmPushSender fcm = montar(builder.build(), par());

        server.expect(requestTo("https://oauth2.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"ya29.tok\"}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://fcm.test/v1/projects/proj-1/messages:send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer ya29.tok"))
                .andExpect(jsonPath("$.message.token").value("token-do-aparelho"))
                .andExpect(jsonPath("$.message.notification.title").value("Dê ciência do seu espelho"))
                .andExpect(jsonPath("$.message.notification.body").value("A competência 07/2026 aguarda."))
                .andRespond(withSuccess());

        fcm.enviar("token-do-aparelho", "Dê ciência do seu espelho", "A competência 07/2026 aguarda.");

        server.verify();
    }

    @Test
    void reaproveitaOAccessTokenEntreEnvios() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FcmPushSender fcm = montar(builder.build(), par());

        // UMA troca de token só; dois envios.
        server.expect(requestTo("https://oauth2.test/token"))
                .andRespond(withSuccess("{\"access_token\":\"ya29.tok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://fcm.test/v1/projects/proj-1/messages:send")).andRespond(withSuccess());
        server.expect(requestTo("https://fcm.test/v1/projects/proj-1/messages:send")).andRespond(withSuccess());

        fcm.enviar("ap1", "t", "m");
        fcm.enviar("ap2", "t", "m");

        server.verify();
    }

    @Test
    void oJwtEAssinadoComAChaveDaContaDeServicoEVerificaComAPublica() throws Exception {
        KeyPair kp = par();
        FcmPushSender fcm = montar(RestClient.builder().build(), kp);

        String jwt = fcm.montarJwt();
        String[] partes = jwt.split("\\.");
        assertThat(partes).hasSize(3);

        String claims = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        assertThat(claims).contains("robo@proj-1.iam.gserviceaccount.com")
                .contains("https://www.googleapis.com/auth/firebase.messaging");

        Signature v = Signature.getInstance("SHA256withRSA");
        v.initVerify(kp.getPublic());
        v.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.UTF_8));
        assertThat(v.verify(Base64.getUrlDecoder().decode(partes[2]))).isTrue();
    }
}
