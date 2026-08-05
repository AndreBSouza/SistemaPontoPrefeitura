package br.gov.ponto.ia;

import br.gov.ponto.common.tenant.TenantContext;
import br.gov.ponto.funcionalidade.FuncionalidadeService;
import br.gov.ponto.tenant.TenantService;
import br.gov.ponto.tenant.api.CriarTenantRequest;
import br.gov.ponto.tenant.domain.TipoPoder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Com um provedor {@code @Primary} plugado, os recursos respondem — provando o seam. */
@SpringBootTest
@AutoConfigureEmbeddedDatabase(
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY,
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD)
@Import(IaSeamTest.FakeIaConfig.class)
class IaSeamTest {

    @TestConfiguration
    static class FakeIaConfig {
        @Bean
        @Primary
        IaProvider fakeIa() {
            return new IaProvider() {
                @Override
                public boolean disponivel() {
                    return true;
                }

                @Override
                public String responderAssistente(String pergunta, UUID vinculoId) {
                    return "resposta para: " + pergunta;
                }

                @Override
                public OcrAtestado lerAtestado(byte[] imagem, String contentType) {
                    return new OcrAtestado("Z00", LocalDate.of(2026, 6, 1), 3, "Dr. Teste", "CRM-GO 123", "atestado");
                }

                @Override
                public String resumirGestao(String contexto) {
                    return "Resumo do mês.";
                }

                @Override
                public SentimentoIa analisarSentimento(java.util.List<String> comentarios) {
                    return new SentimentoIa(comentarios.size(), 0, 0, "resumo de sentimento");
                }
            };
        }
    }

    @Autowired
    private TenantService tenantService;
    @Autowired
    private FuncionalidadeService funcionalidadeService;
    @Autowired
    private AssistenteService assistenteService;
    @Autowired
    private OcrAtestadoService ocrAtestadoService;
    @Autowired
    private ResumoIaService resumoIaService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        var id = tenantService.criar(new CriarTenantRequest("Ente IA2", "ente-ia2", TipoPoder.EXECUTIVO)).id();
        TenantContext.set(id.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void desligadoContinuaIndisponivelMesmoComProvedor() {
        // Provedor existe, mas o ente não ligou a função -> indisponível.
        assertThat(assistenteService.perguntar("oi", UUID.randomUUID()).disponivel()).isFalse();
    }

    @Test
    void ligadoComProvedorResponde() {
        funcionalidadeService.definir("IA_ASSISTENTE", true);
        funcionalidadeService.definir("IA_OCR", true);
        funcionalidadeService.definir("IA_RESUMO", true);

        var assist = assistenteService.perguntar("quantas horas tenho?", UUID.randomUUID());
        assertThat(assist.disponivel()).isTrue();
        assertThat(assist.resposta()).contains("resposta para");

        var ocr = ocrAtestadoService.ler(new byte[]{1, 2, 3}, "image/png");
        assertThat(ocr.disponivel()).isTrue();
        assertThat(ocr.dados().cid()).isEqualTo("Z00");

        var resumo = resumoIaService.resumir(YearMonth.of(2026, 6));
        assertThat(resumo.disponivel()).isTrue();
        assertThat(resumo.resumo()).isNotBlank();
    }
}
