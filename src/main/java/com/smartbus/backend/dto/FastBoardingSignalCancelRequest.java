package com.smartbus.backend.dto;

public class FastBoardingSignalCancelRequest {

    private Long signalId;
    private Long tripId;
    private Long routeId;
    private Long destinationStopId;
    private String passengerIdentifier;

    public Long getSignalId() { return signalId; }
    public Long getTripId() { return tripId; }
    public Long getRouteId() { return routeId; }
    public Long getDestinationStopId() { return destinationStopId; }
    public String getPassengerIdentifier() { return passengerIdentifier; }
}
