package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public class AiContextSyncRequest {

    private Long tripId;
    private Long routeId;

    @NotEmpty
    private Map<String, Object> context;

    /** Monotonic client value; an old retry must not overwrite a newer snapshot. */
    private Long clientVersion;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    public Long getClientVersion() { return clientVersion; }
    public void setClientVersion(Long clientVersion) { this.clientVersion = clientVersion; }
}
