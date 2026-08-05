package br.gov.ponto.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Registra o provedor de IA real (OpenAI) SOMENTE quando {@code ia.api-key} está definido
 * (via {@code IA_API_KEY} no ambiente/secret). Sem a chave, o bean não é criado e prevalece o
 * no-op {@link IaIndisponivel} — a IA responde "indisponível" de forma segura, sem quebrar nada.
 */
@Configuration
public class IaConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "ia", name = "api-key")
    public IaProvider openAiIaProvider(
            @Value("${ia.api-key:}") String apiKey,
            @Value("${ia.base-url:https://api.openai.com}") String baseUrl,
            @Value("${ia.modelo:gpt-4o-mini}") String modelo,
            @Value("${ia.max-tokens:1024}") int maxTokens,
            @Value("${ia.permitir-externo:true}") boolean permitirExterno,
            RestClient.Builder builder,
            ObjectMapper mapper) {
        RestClient rest = builder.baseUrl(baseUrl).requestFactory(timeouts()).build();
        return new OpenAiIaProvider(rest, apiKey, modelo, maxTokens, mapper, permitirExterno);
    }

    private static ClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return factory;
    }
}
