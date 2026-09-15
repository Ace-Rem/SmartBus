package com.smartbus.backend.dto;

public class FastBoardingSignalAcceptRequest {

    private Long tripId;
    private Long routeId;
    private String passengerIdentifier;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    public String getPassengerIdentifier() { return passengerIdentifier; }
    public void setPassengerIdentifier(String passengerIdentifier) { this.passengerIdentifier = passengerIdentifier; }
}
