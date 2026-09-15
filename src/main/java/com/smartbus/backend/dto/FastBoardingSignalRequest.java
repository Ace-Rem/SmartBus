package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FastBoardingSignalRequest {

    @NotNull
    @Positive
    private Long routeId;

    @NotNull
    @Positive
    private Long destinationStopId;

    private Long tripId;
    private Long boardingStopId;
    private String passengerIdentifier;

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Long getDestinationStopId() {
        return destinationStopId;
    }

    public void setDestinationStopId(Long destinationStopId) {
        this.destinationStopId = destinationStopId;
    }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getBoardingStopId() { return boardingStopId; }
    public void setBoardingStopId(Long boardingStopId) { this.boardingStopId = boardingStopId; }
    public String getPassengerIdentifier() { return passengerIdentifier; }
    public void setPassengerIdentifier(String passengerIdentifier) { this.passengerIdentifier = passengerIdentifier; }
}
