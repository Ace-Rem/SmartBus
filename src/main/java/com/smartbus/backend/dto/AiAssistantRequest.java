package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class AiAssistantRequest {

    @NotNull
    private Long tripId;

    @NotBlank
    private String question;

    private Map<String, Object> clientContext;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Map<String, Object> getClientContext() {
        return clientContext;
    }

    public void setClientContext(Map<String, Object> clientContext) {
        this.clientContext = clientContext;
    }
}
