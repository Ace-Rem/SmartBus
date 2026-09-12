package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class AiContextSyncRequest {

    @NotNull
    private Long tripId;

    @NotEmpty
    private Map<String, Object> context;

    /** Monotonic client value; an old retry must not overwrite a newer snapshot. */
    private Long clientVersion;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    public Long getClientVersion() { return clientVersion; }
    public void setClientVersion(Long clientVersion) { this.clientVersion = clientVersion; }
}
