package com.smartbus.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartbus.backend.config.AiProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Provider adapter for OpenAI-compatible APIs and Gemini native function calling. */
@Component
public class OpenAiClient implements AiClient {
    public static final String FALLBACK_MARKER = "__AI_FALLBACK__";
    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private final AiProperties properties;
    private final ObjectMapper mapper;
    private final RestClient restClient;

    public OpenAiClient(AiProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder().baseUrl(effectiveBaseUrl())
                .requestFactory(factory).build();
        log.info("AI adapter ready: provider={}, model={}, apiKeyConfigured={}",
                properties.getProvider(), properties.getModel(), hasKey());
    }

    @Override
    public AiResponse complete(AiRequest request) {
        if (!hasKey()) return fallbackResponse();
        try {
            return isGemini() ? callGemini(request) : callOpenAiCompatible(request);
        } catch (RestClientException | java.io.IOException exception) {
            log.warn("AI provider request failed: {}", safeMessage(exception));
            return fallbackResponse();
        }
    }

    private AiResponse callOpenAiCompatible(AiRequest request) throws java.io.IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", effectiveModel());
        body.put("temperature", 0.2);
        body.put("max_tokens", 700);
        body.set("messages", mapper.valueToTree(request.getMessages()));
        if (!request.getTools().isEmpty()) body.set("tools", mapper.valueToTree(request.getTools()));
        JsonNode root = postOpenAi(body);
        JsonNode message = root.path("choices").path(0).path("message");
        AiResponse result = new AiResponse();
        result.setContent(textOrNull(message.path("content")));
        if (message.has("tool_calls")) {
            result.setToolCalls(mapper.convertValue(message.path("tool_calls"), List.class));
        }
        return result;
    }

    private JsonNode postOpenAi(ObjectNode body) throws java.io.IOException {
        String key = stripWrappingQuotes(properties.getApiKey().trim());
        String raw = restClient.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + key)
                .header("HTTP-Referer", "https://smartbus.local")
                .header("X-Title", "SmartBus AI Assistant").body(body)
                .exchange((request, response) -> response.bodyTo(String.class));
        JsonNode root = mapper.readTree(raw == null ? "" : raw);
        if (raw == null || raw.isBlank() || root.has("error")) {
            throw new RestClientException("AI provider returned an error");
        }
        return root;
    }

    /** Gemini uses the same logical tool contract, translated to functionDeclarations. */
    private AiResponse callGemini(AiRequest request) throws java.io.IOException {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        StringBuilder systemText = new StringBuilder();
        for (Map<String, Object> message : request.getMessages()) {
            String role = String.valueOf(message.getOrDefault("role", "user"));
            if ("system".equals(role)) {
                if (systemText.length() > 0) systemText.append('\n');
                systemText.append(String.valueOf(message.getOrDefault("content", "")));
                continue;
            }
            ObjectNode content = contents.addObject();
            if ("tool".equals(role)) {
                content.put("role", "user");
                ObjectNode response = content.putArray("parts").addObject().putObject("functionResponse");
                response.put("name", String.valueOf(message.getOrDefault("name", "smartbus_tool")));
                try { response.set("response", mapper.readTree(String.valueOf(message.getOrDefault("content", "{}")))); }
                catch (Exception ignored) { response.put("response", String.valueOf(message.getOrDefault("content", ""))); }
            } else {
                content.put("role", "assistant".equals(role) ? "model" : "user");
                if (message.get("tool_calls") instanceof List<?> calls) {
                    ArrayNode parts = content.putArray("parts");
                    for (Object rawCall : calls) {
                        if (!(rawCall instanceof Map<?, ?> call)) continue;
                        Object function = call.get("function");
                        if (!(function instanceof Map<?, ?> fn)) continue;
                        ObjectNode part = parts.addObject().putObject("functionCall");
                        part.put("name", String.valueOf(fn.get("name") == null ? "" : fn.get("name")));
                        Object args = fn.get("arguments");
                        if (args instanceof String text) {
                            try { part.set("args", mapper.readTree(text)); } catch (Exception ignored) { part.putObject("args"); }
                        } else if (args != null) part.set("args", mapper.valueToTree(args));
                    }
                } else {
                    content.putArray("parts").addObject().put("text", String.valueOf(message.getOrDefault("content", "")));
                }
            }
        }
        if (systemText.length() > 0) {
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", systemText.toString());
        }
        if (!request.getTools().isEmpty()) {
            ArrayNode declarations = mapper.createArrayNode();
            for (Map<String, Object> tool : request.getTools()) {
                Object function = tool.get("function");
                if (function instanceof Map<?, ?> value) declarations.add(mapper.valueToTree(value));
            }
            body.putArray("tools").addObject().set("functionDeclarations", declarations);
        }
        String uri = effectiveBaseUrl() + "/models/" + effectiveModel()
                + ":generateContent?key=" + stripWrappingQuotes(properties.getApiKey().trim());
        String raw = restClient.post().uri(URI.create(uri)).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(String.class);
        JsonNode root = mapper.readTree(raw == null ? "" : raw);
        if (raw == null || raw.isBlank() || root.has("error")) throw new RestClientException("Gemini error");
        AiResponse result = new AiResponse();
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        List<Map<String, Object>> calls = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) text.append(part.path("text").asText());
            if (part.has("functionCall")) {
                Map<String, Object> call = new java.util.LinkedHashMap<>();
                call.put("id", "gemini-" + calls.size());
                call.put("type", "function");
                call.put("function", mapper.convertValue(part.path("functionCall"), Map.class));
                calls.add(call);
            }
        }
        result.setContent(text.toString());
        result.setToolCalls(calls);
        return result;
    }

    private boolean isGemini() { return "gemini".equalsIgnoreCase(properties.getProvider()); }
    private String effectiveModel() {
        if (properties.getModel() == null || properties.getModel().isBlank()
                || (isGemini() && properties.getModel().startsWith("deepseek/"))) {
            return isGemini() ? "gemini-2.0-flash" : "deepseek/deepseek-chat";
        }
        return properties.getModel().trim();
    }
    private String effectiveBaseUrl() {
        if (isGemini() && (properties.getBaseUrl() == null || properties.getBaseUrl().contains("openrouter.ai"))) {
            return "https://generativelanguage.googleapis.com/v1beta";
        }
        return trimTrailingSlash(properties.getBaseUrl());
    }
    private boolean hasKey() { return properties.getApiKey() != null && !properties.getApiKey().isBlank(); }
    private AiResponse fallbackResponse() { AiResponse r = new AiResponse(); r.setContent(FALLBACK_MARKER); return r; }
    public static boolean isUnavailableApology(String content) {
        return content == null || content.isBlank() || FALLBACK_MARKER.equals(content.trim());
    }
    private static String textOrNull(JsonNode node) { return node == null || node.isNull() ? null : node.asText(null); }
    private static String stripWrappingQuotes(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) return value.substring(1, value.length() - 1).trim();
        return value;
    }
    private static String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null ? exception.getClass().getSimpleName()
                : message.replaceAll("(?i)Bearer\\s+\\S+", "Bearer ***");
    }
    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://openrouter.ai/api/v1";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
