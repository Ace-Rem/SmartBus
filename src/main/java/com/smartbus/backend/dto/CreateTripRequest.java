package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotNull;

public class CreateTripRequest {

    @NotNull
    private Long routeId;

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }
}
