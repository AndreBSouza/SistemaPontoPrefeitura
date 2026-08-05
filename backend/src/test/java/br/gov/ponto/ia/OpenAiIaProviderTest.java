package br.gov.ponto.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Testa o provedor OpenAI contra respostas canned (MockRestServiceServer) — sem rede. */
class OpenAiIaProviderTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private OpenAiIaProvider provider(RestClient.Builder builder, String key) {
        return new OpenAiIaProvider(builder.baseUrl("https://api.openai.com").build(), key, "gpt-4o-mini", 512, mapper, true);
    }

    @Test
    void killSwitchExternoDesligaMesmoComChave() {
        OpenAiIaProvider bloqueado = new OpenAiIaProvider(
                RestClient.builder().baseUrl("https://api.openai.com").build(), "sk-x", "gpt-4o-mini", 512, mapper, false);
        assertThat(bloqueado.disponivel()).isFalse();
    }

    /** Resposta da OpenAI cujo choices[0].message.content é o texto informado. */
    private String respostaChat(String content) throws Exception {
        return mapper.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of("content", content)))));
    }

    @Test
    void disponivelSomenteComChave() {
        RestClient.Builder b = RestClient.builder();
        assertThat(provider(b, "").disponivel()).isFalse();
        assertThat(provider(b, "sk-x").disponivel()).isTrue();
    }

    @Test
    void assistenteRetornaTextoEEnviaAutorizacao() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andRespond(withSuccess(respostaChat("Olá, servidor!"), MediaType.APPLICATION_JSON));

        OpenAiIaProvider p = provider(builder, "sk-test");
        assertThat(p.responderAssistente("oi", UUID.randomUUID())).isEqualTo("Olá, servidor!");
        server.verify();
    }

    @Test
    void ocrParseiaJsonDoConteudo() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String jsonAtestado = mapper.writeValueAsString(Map.of(
                "cid", "J11", "inicio", "2026-06-10", "dias", 3,
                "profissional", "Dra. Ana", "registro", "CRM-GO 123"));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(respostaChat(jsonAtestado), MediaType.APPLICATION_JSON));

        OpenAiIaProvider p = provider(builder, "sk-test");
        OcrAtestado r = p.lerAtestado(new byte[]{1, 2, 3}, "image/png");
        assertThat(r.cid()).isEqualTo("J11");
        assertThat(r.inicio()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(r.dias()).isEqualTo(3);
        assertThat(r.profissional()).isEqualTo("Dra. Ana");
        server.verify();
    }

    @Test
    void ocrComTextoNaoEstruturadoNaoQuebra() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(respostaChat("não consegui ler o atestado"), MediaType.APPLICATION_JSON));

        OpenAiIaProvider p = provider(builder, "sk-test");
        OcrAtestado r = p.lerAtestado(new byte[]{9}, "image/jpeg");
        assertThat(r.cid()).isNull();
        assertThat(r.dias()).isZero();
        assertThat(r.textoBruto()).contains("não consegui");
    }

    @Test
    void erroDaApiViraIaFalha() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withServerError());

        OpenAiIaProvider p = provider(builder, "sk-test");
        assertThatThrownBy(() -> p.resumirGestao("ctx")).isInstanceOf(IaFalhaException.class);
    }

    @Test
    void sentimentoParseiaContagens() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String json = mapper.writeValueAsString(Map.of(
                "positivos", 2, "neutros", 1, "negativos", 0, "resumo", "no geral positivo"));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(respostaChat(json), MediaType.APPLICATION_JSON));

        OpenAiIaProvider p = provider(builder, "sk-test");
        SentimentoIa s = p.analisarSentimento(List.of("bom", "ok", "regular"));
        assertThat(s.positivos()).isEqualTo(2);
        assertThat(s.neutros()).isEqualTo(1);
        assertThat(s.negativos()).isZero();
        assertThat(s.resumo()).contains("positivo");
        server.verify();
    }

    @Test
    void ocrRejeitaFormatoNaoSuportado() {
        OpenAiIaProvider p = provider(RestClient.builder(), "sk-test");
        assertThatThrownBy(() -> p.lerAtestado(new byte[]{1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ocrAceitaDataBrasileira() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String jsonAtestado = mapper.writeValueAsString(Map.of("cid", "J11", "inicio", "10/06/2026", "dias", 2));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(respostaChat(jsonAtestado), MediaType.APPLICATION_JSON));

        OpenAiIaProvider p = provider(builder, "sk-test");
        assertThat(p.lerAtestado(new byte[]{1}, "image/jpg").inicio()).isEqualTo(LocalDate.of(2026, 6, 10));
        server.verify();
    }

    @Test
    void respostaSemConteudoTextualViraIaFalha() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // choices[0].message sem "content" textual.
        String corpo = mapper.writeValueAsString(Map.of("choices", List.of(Map.of("message", Map.of()))));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));

        OpenAiIaProvider p = provider(builder, "sk-test");
        assertThatThrownBy(() -> p.responderAssistente("oi", UUID.randomUUID())).isInstanceOf(IaFalhaException.class);
    }
}
