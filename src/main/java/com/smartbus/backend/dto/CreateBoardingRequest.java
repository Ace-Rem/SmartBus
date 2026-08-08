package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotNull;

public class CreateBoardingRequest {

    @NotNull
    private Long tripId;

    @NotNull
    private Long boardingStopId;

    @NotNull
    private Long destinationStopId;

    private String note;

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public Long getBoardingStopId() { return boardingStopId; }
    public void setBoardingStopId(Long boardingStopId) { this.boardingStopId = boardingStopId; }
    public Long getDestinationStopId() { return destinationStopId; }
    public void setDestinationStopId(Long destinationStopId) { this.destinationStopId = destinationStopId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
