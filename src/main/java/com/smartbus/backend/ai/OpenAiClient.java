package com.smartbus.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartbus.backend.config.AiProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * OpenAI-compatible chat client for OpenRouter (DeepSeek).
 * On any provider failure returns {@link #FALLBACK_MARKER} so the service layer
 * can answer from DB context — never a generic "Xin loi..." string.
 */
@Component
public class OpenAiClient implements AiClient {

    public static final String FALLBACK_MARKER = "__AI_FALLBACK__";

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(aiProperties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();

        String key = aiProperties.getApiKey();
        boolean keyPresent = key != null && !key.isBlank();
        log.info("AI client ready: provider={}, model={}, baseUrl={}, apiKeyConfigured={}",
                aiProperties.getProvider(),
                aiProperties.getModel(),
                aiProperties.getBaseUrl(),
                keyPresent);
        if (!keyPresent) {
            log.warn("AI_API_KEY is empty — chat will use DB data-driven answers until key is set on Render");
        }
    }

    @Override
    public AiResponse complete(AiRequest request) {
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackResponse();
        }
        apiKey = stripWrappingQuotes(apiKey.trim());
        if (apiKey.isBlank()) {
            return fallbackResponse();
        }

        for (String model : candidateModels()) {
            try {
                String content = callProvider(apiKey, model, request.getPrompt());
                if (content != null && !content.isBlank() && !isUnavailableApology(content)) {
                    AiResponse response = new AiResponse();
                    response.setContent(content.trim());
                    return response;
                }
                log.warn("AI model={} returned empty/apology content; trying next candidate", model);
            } catch (RestClientException | java.io.IOException ex) {
                log.warn("AI model={} failed: {} — {}", model, ex.getClass().getSimpleName(), safeMessage(ex));
            }
        }
        return fallbackResponse();
    }

    private String callProvider(String apiKey, String model, String prompt)
            throws java.io.IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("max_tokens", 900);
        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", "Ban la tro ly SmartBus cho tai xe xe buyt. "
                + "Tra loi bang tieng Viet, ngan gon, de hieu. "
                + "Chi dung CONTEXT trong prompt. Khong bia so lieu. "
                + "Khong bao gio tra loi cau xin loi chung chung neu CONTEXT da co du lieu.");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", prompt);

        return restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .header("HTTP-Referer", "https://smartbus.local")
                .header("X-Title", "SmartBus Driver Assistant")
                .body(body)
                .exchange((request, response) -> {
                    String responseBody = response.bodyTo(String.class);
                    if (responseBody == null || responseBody.isBlank()) {
                        throw new RestClientException("Empty body from AI provider, status="
                                + response.getStatusCode());
                    }
                    JsonNode root = objectMapper.readTree(responseBody);
                    if (!response.getStatusCode().is2xxSuccessful() || root.has("error")) {
                        String errMsg = root.path("error").path("message").asText(responseBody);
                        throw new RestClientException("AI HTTP "
                                + response.getStatusCode().value() + ": " + errMsg);
                    }
                    return root.path("choices").path(0).path("message").path("content").asText("");
                });
    }

    private List<String> candidateModels() {
        Set<String> models = new LinkedHashSet<>();
        if (aiProperties.getModel() != null && !aiProperties.getModel().isBlank()) {
            models.add(aiProperties.getModel().trim());
        }
        models.add("deepseek/deepseek-chat");
        models.add("deepseek/deepseek-chat-v3-0324");
        models.add("deepseek/deepseek-v3.2");
        return new ArrayList<>(models);
    }

    private AiResponse fallbackResponse() {
        AiResponse fallback = new AiResponse();
        fallback.setContent(FALLBACK_MARKER);
        return fallback;
    }

    public static boolean isUnavailableApology(String content) {
        if (content == null) {
            return false;
        }
        String normalized = content.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("xin loi, tro ly ai")
                || normalized.contains("xin lỗi, trợ lý ai")
                || normalized.contains("tam thoi khong kha dung")
                || normalized.contains("tạm thời không khả dụng")
                || FALLBACK_MARKER.equals(content.trim());
    }

    private static String stripWrappingQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message == null) {
            return "";
        }
        return message.replaceAll("(?i)Bearer\\s+\\S+", "Bearer ***");
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://openrouter.ai/api/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
