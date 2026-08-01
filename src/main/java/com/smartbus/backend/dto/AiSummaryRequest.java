package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotNull;

public class AiSummaryRequest {

    @NotNull
    private Long tripId;

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }
}
