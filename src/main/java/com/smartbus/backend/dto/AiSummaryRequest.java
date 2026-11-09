package com.smartbus.backend.dto;

import java.util.Map;
import java.util.List;

public class AiSummaryRequest {

    private Long tripId;
    private Long routeId;

    private Map<String, Object> clientContext;
    private List<AiConversationMessage> conversationHistory;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }

    public Map<String, Object> getClientContext() {
        return clientContext;
    }

    public void setClientContext(Map<String, Object> clientContext) {
        this.clientContext = clientContext;
    }

    public List<AiConversationMessage> getConversationHistory() { return conversationHistory; }
    public void setConversationHistory(List<AiConversationMessage> conversationHistory) { this.conversationHistory = conversationHistory; }
}
