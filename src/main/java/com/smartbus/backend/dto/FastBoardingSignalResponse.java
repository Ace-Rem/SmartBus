package com.smartbus.backend.dto;

public class FastBoardingSignalResponse {

    private Long id;
    private Long routeId;
    private Long destinationStopId;

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
}
