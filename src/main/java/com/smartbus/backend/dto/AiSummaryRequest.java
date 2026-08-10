package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class AiSummaryRequest {

    @NotNull
    private Long tripId;

    private Map<String, Object> clientContext;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Map<String, Object> getClientContext() {
        return clientContext;
    }

    public void setClientContext(Map<String, Object> clientContext) {
        this.clientContext = clientContext;
    }
}
