package com.smartbus.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartbus.backend.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiClient(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .build();
    }

    @Override
    public AiResponse complete(AiRequest request) {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            AiResponse fallback = new AiResponse();
            fallback.setContent(buildFallback());
            return fallback;
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", aiProperties.getModel());
            ArrayNode messages = body.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", "You are a helpful Vietnamese bus-driver assistant. "
                    + "Answer in natural language only. Do not invent business rules or change data.");
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", request.getPrompt());

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            AiResponse response = new AiResponse();
            response.setContent(content.isBlank() ? buildFallback() : content.trim());
            return response;
        } catch (RestClientException | java.io.IOException ex) {
            log.warn("AI provider call failed; using fallback response");
            AiResponse fallback = new AiResponse();
            fallback.setContent(buildFallback());
            return fallback;
        }
    }

    private String buildFallback() {
        return "Xin loi, tro ly AI tam thoi khong kha dung. "
                + "Vui long thu lai sau. He thong van dang theo doi chuyen xe binh thuong.";
    }
}
