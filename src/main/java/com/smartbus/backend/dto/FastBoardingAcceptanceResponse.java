package com.smartbus.backend.dto;

import java.time.LocalDateTime;

public class FastBoardingAcceptanceResponse {

    private Long id;
    private Long signalId;
    private Long passengerId;
    private Long tripId;
    private Long routeId;
    private Long boardingStopId;
    private Long destinationStopId;
    private String status;
    private LocalDateTime acceptedAt;

    public FastBoardingAcceptanceResponse() { }

    public FastBoardingAcceptanceResponse(Long id, Long signalId, Long passengerId, Long tripId,
                                          Long routeId, Long boardingStopId, Long destinationStopId,
                                          String status, LocalDateTime acceptedAt) {
        this.id = id;
        this.signalId = signalId;
        this.passengerId = passengerId;
        this.tripId = tripId;
        this.routeId = routeId;
        this.boardingStopId = boardingStopId;
        this.destinationStopId = destinationStopId;
        this.status = status;
        this.acceptedAt = acceptedAt;
    }

    public Long getId() { return id; }
    public Long getSignalId() { return signalId; }
    public Long getPassengerId() { return passengerId; }
    public Long getTripId() { return tripId; }
    public Long getRouteId() { return routeId; }
    public Long getBoardingStopId() { return boardingStopId; }
    public Long getDestinationStopId() { return destinationStopId; }
    public String getStatus() { return status; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
}
