package br.gov.ponto.ia;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Com {@code ia.api-key} definido, o provedor real (OpenAI) é criado e vira o {@code @Primary} —
 * prova que a fiação condicional + @Primary funciona no contexto real (sem a chave, {@link IaGatingTest}
 * mostra que prevalece o no-op). A chave aqui é fake e nenhum request de rede é feito.
 */
@SpringBootTest(properties = "ia.api-key=sk-test-somente-wiring")
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
class IaProviderWiringTest {

    @Autowired
    private IaProvider provider;

    @Test
    void comChaveOProvedorPrimarioEhOpenAi() {
        assertThat(provider).isInstanceOf(OpenAiIaProvider.class);
        assertThat(provider.disponivel()).isTrue();
    }
}
