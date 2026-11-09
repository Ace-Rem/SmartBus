package com.smartbus.backend.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiRequest {

    private String prompt;
    private List<Map<String, Object>> messages = new ArrayList<>();
    private List<Map<String, Object>> tools = new ArrayList<>();

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public List<Map<String, Object>> getMessages() {
        return messages;
    }

    public void setMessages(List<Map<String, Object>> messages) {
        this.messages = messages == null ? new ArrayList<>() : messages;
    }

    public List<Map<String, Object>> getTools() {
        return tools;
    }

    public void setTools(List<Map<String, Object>> tools) {
        this.tools = tools == null ? new ArrayList<>() : tools;
    }

    public AiRequest addMessage(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        messages.add(message);
        return this;
    }
}
