package com.smartbus.backend.dto;

public class FastBoardingSignalResponse {

    private Long id;
    private Long routeId;
    private Long destinationStopId;
    private Long tripId;
    private Long boardingStopId;
    private Long passengerId;
    private String passengerIdentifier;

    public FastBoardingSignalResponse() {
    }

    public FastBoardingSignalResponse(Long id, Long routeId, Long destinationStopId) {
        this.id = id;
        this.routeId = routeId;
        this.destinationStopId = destinationStopId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
    public Long getPassengerId() { return passengerId; }
    public void setPassengerId(Long passengerId) { this.passengerId = passengerId; }
    public String getPassengerIdentifier() { return passengerIdentifier; }
    public void setPassengerIdentifier(String passengerIdentifier) { this.passengerIdentifier = passengerIdentifier; }
}
