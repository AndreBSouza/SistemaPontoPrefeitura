package br.gov.ponto.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Provedor de IA sobre a API da OpenAI (Chat Completions, com visão para o OCR).
 *
 * <p>Registrado como {@code @Primary} apenas quando {@code ia.api-key} está definido (ver
 * {@link IaConfig}); sem chave, vale o no-op {@link IaIndisponivel} e a IA fica "indisponível".
 * A chave vem de variável de ambiente/secret ({@code IA_API_KEY}) — <b>nunca do código</b>.</p>
 */
public class OpenAiIaProvider implements IaProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiIaProvider.class);
    private static final String CHAT = "/v1/chat/completions";
    /** Formatos de imagem aceitos pela API de visão da OpenAI. */
    private static final Set<String> MIME_SUPORTADOS = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
    /** Datas em atestados: ISO e o formato brasileiro. */
    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu"),
            DateTimeFormatter.ofPattern("d/M/uuuu"));

    private final RestClient rest;
    private final String apiKey;
    private final String modelo;
    private final int maxTokens;
    private final ObjectMapper mapper;
    private final boolean permitirExterno;

    public OpenAiIaProvider(RestClient rest, String apiKey, String modelo, int maxTokens,
                            ObjectMapper mapper, boolean permitirExterno) {
        this.rest = rest;
        // strip(): secrets montados de arquivo (k8s/Docker) costumam vir com \n final, que
        // corromperia o header Authorization (401/exceção). Nunca há espaço/quebra numa chave válida.
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.modelo = modelo;
        this.maxTokens = maxTokens;
        this.mapper = mapper;
        this.permitirExterno = permitirExterno;
    }

    @Override
    public boolean disponivel() {
        // Kill-switch org (LGPD): ia.permitir-externo=false desliga a IA em todo o ambiente,
        // independentemente da chave e das flags por ente. Útil p/ controle interno/jurídico.
        return !apiKey.isBlank() && permitirExterno;
    }

    @Override
    public String responderAssistente(String pergunta, UUID vinculoId) {
        String sistema = "Você é o assistente de um sistema de ponto eletrônico de uma prefeitura. "
                + "Responda em português do Brasil, de forma curta, cordial e objetiva. "
                + "Não invente dados de banco de horas, escalas ou saldos: se não tiver a informação, "
                + "oriente o servidor a consultar a tela correspondente ou o RH.";
        Object payload = Map.of(
                "model", modelo,
                "max_completion_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", sistema),
                        Map.of("role", "user", "content", pergunta)));
        return conteudoDoTexto(chamar(payload));
    }

    @Override
    public OcrAtestado lerAtestado(byte[] imagem, String contentType) {
        String mime = normalizarMime(contentType);
        if (!MIME_SUPORTADOS.contains(mime)) {
            throw new IllegalArgumentException(
                    "Formato de imagem não suportado para OCR: " + mime + ". Envie PNG, JPG, GIF ou WEBP.");
        }
        String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imagem);
        String instrucao = "Extraia os dados deste atestado médico e responda APENAS com um JSON "
                + "(sem markdown, sem comentários) com as chaves: cid (string ou null), "
                + "inicio (data no formato yyyy-MM-dd ou null), dias (inteiro; 0 se não informado), "
                + "profissional (nome do médico ou null), registro (CRM ou null).";
        Object conteudo = List.of(
                Map.of("type", "text", "text", instrucao),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUri)));
        Object payload = Map.of(
                "model", modelo,
                "max_completion_tokens", maxTokens,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(Map.of("role", "user", "content", conteudo)));
        return parseAtestado(conteudoDoTexto(chamar(payload)));
    }

    @Override
    public SentimentoIa analisarSentimento(List<String> comentarios) {
        // Teto duro de segurança (custo/tamanho do prompt): amostra os primeiros N e trunca cada um.
        // A camada de serviço já limita, mas o provedor se protege de qualquer chamador.
        final int maxComentarios = 400;
        final int maxTamanho = 400;
        List<String> usados = comentarios.size() > maxComentarios
                ? comentarios.subList(0, maxComentarios) : comentarios;

        String sistema = "Você analisa o sentimento de comentários de servidores públicos sobre o sistema de "
                + "ponto eletrônico. Classifique cada comentário como positivo, neutro ou negativo.";
        StringBuilder lista = new StringBuilder();
        for (int i = 0; i < usados.size(); i++) {
            String c = usados.get(i);
            if (c != null && c.length() > maxTamanho) {
                c = c.substring(0, maxTamanho);
            }
            lista.append(i + 1).append(". ").append(c).append('\n');
        }
        String instrucao = "Comentários:\n" + lista + "\nResponda APENAS com um JSON (sem markdown) com as chaves: "
                + "positivos (inteiro), neutros (inteiro), negativos (inteiro) — a soma deve ser " + usados.size()
                + " — e resumo (2 a 3 frases em português destacando os temas recorrentes).";
        Object payload = Map.of(
                "model", modelo,
                "max_completion_tokens", maxTokens,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", sistema),
                        Map.of("role", "user", "content", instrucao)));
        JsonNode j;
        try {
            j = mapper.readTree(extrairJson(conteudoDoTexto(chamar(payload))));
        } catch (IaFalhaException e) {
            throw e;
        } catch (Exception e) {
            throw new IaFalhaException("Resposta de análise de sentimento inválida.");
        }
        String resumo = textoOuNull(j, "resumo");
        return new SentimentoIa(
                j.path("positivos").asInt(0),
                j.path("neutros").asInt(0),
                j.path("negativos").asInt(0),
                resumo == null ? "" : resumo);
    }

    @Override
    public String resumirGestao(String contexto) {
        String sistema = "Você resume indicadores de frequência de servidores públicos para um gestor. "
                + "Escreva em português do Brasil, em 3 a 5 frases, tom executivo, destacando hora extra, "
                + "absenteísmo e pontos de atenção. Não invente números além do contexto fornecido.";
        Object payload = Map.of(
                "model", modelo,
                "max_completion_tokens", maxTokens,
                "messages", List.of(
                        Map.of("role", "system", "content", sistema),
                        Map.of("role", "user", "content", contexto)));
        return conteudoDoTexto(chamar(payload));
    }

    private JsonNode chamar(Object payload) {
        try {
            return rest.post()
                    .uri(CHAT)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException e) {
            // Qualquer falha da chamada externa (rede, 4xx/5xx, header inválido) vira 503 gracioso.
            // Loga só o tipo — nunca a mensagem, que poderia conter um fragmento da chave/header.
            log.warn("Falha ao chamar o provedor de IA (OpenAI): {}", e.getClass().getSimpleName());
            throw new IaFalhaException(e);
        }
    }

    private static String conteudoDoTexto(JsonNode resposta) {
        if (resposta == null) {
            throw new IaFalhaException("Resposta vazia do provedor de IA.");
        }
        JsonNode c = resposta.path("choices").path(0).path("message").path("content");
        if (!c.isTextual()) {
            throw new IaFalhaException("Resposta do provedor de IA sem conteúdo textual.");
        }
        return c.asText();
    }

    private OcrAtestado parseAtestado(String texto) {
        try {
            JsonNode j = mapper.readTree(extrairJson(texto));
            return new OcrAtestado(
                    textoOuNull(j, "cid"),
                    parseData(textoOuNull(j, "inicio")),
                    j.path("dias").asInt(0),
                    textoOuNull(j, "profissional"),
                    textoOuNull(j, "registro"),
                    texto);
        } catch (Exception e) {
            // Não conseguiu estruturar: devolve o texto bruto para o RH revisar manualmente.
            return new OcrAtestado(null, null, 0, null, null, texto);
        }
    }

    /** Extrai o bloco JSON de um texto que pode vir com cercas markdown ou preâmbulo em prosa. */
    private static String extrairJson(String s) {
        String t = s.strip();
        int ini = t.indexOf('{');
        int fim = t.lastIndexOf('}');
        return (ini >= 0 && fim > ini) ? t.substring(ini, fim + 1) : t;
    }

    private static String normalizarMime(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "image/jpeg";
        }
        String m = contentType.toLowerCase().trim();
        // Alguns clientes enviam image/jpg; a OpenAI espera image/jpeg.
        return m.equals("image/jpg") ? "image/jpeg" : m;
    }

    private static String textoOuNull(JsonNode j, String campo) {
        JsonNode n = j.get(campo);
        if (n == null || n.isNull()) {
            return null;
        }
        String v = n.asText();
        return (v == null || v.isBlank()) ? null : v;
    }

    private static LocalDate parseData(String s) {
        if (s == null) {
            return null;
        }
        String v = s.strip();
        for (DateTimeFormatter fmt : FORMATOS_DATA) {
            try {
                return LocalDate.parse(v, fmt);
            } catch (DateTimeParseException ignored) {
                // tenta o próximo formato
            }
        }
        return null;
    }
}
