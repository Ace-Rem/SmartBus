package com.smartbus.backend.dto;

import java.time.LocalDateTime;

public class BoardingRequestResponse {

    private Long id;
    private PassengerResponse passenger;
    private TripResponse trip;
    private StopResponse boardingStop;
    private StopResponse destinationStop;
    private Long passengerRecordId;
    private String status;
    private String note;
    private String bluetoothIdentifier;
    private LocalDateTime requestedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PassengerResponse getPassenger() { return passenger; }
    public void setPassenger(PassengerResponse passenger) { this.passenger = passenger; }
    public TripResponse getTrip() { return trip; }
    public void setTrip(TripResponse trip) { this.trip = trip; }
    public StopResponse getBoardingStop() { return boardingStop; }
    public void setBoardingStop(StopResponse boardingStop) { this.boardingStop = boardingStop; }
    public StopResponse getDestinationStop() { return destinationStop; }
    public void setDestinationStop(StopResponse destinationStop) { this.destinationStop = destinationStop; }
    public Long getPassengerRecordId() { return passengerRecordId; }
    public void setPassengerRecordId(Long passengerRecordId) { this.passengerRecordId = passengerRecordId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getBluetoothIdentifier() { return bluetoothIdentifier; }
    public void setBluetoothIdentifier(String bluetoothIdentifier) { this.bluetoothIdentifier = bluetoothIdentifier; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
