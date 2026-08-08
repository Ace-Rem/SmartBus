package com.smartbus.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "boarding_requests")
public class BoardingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boarding_stop_id", nullable = false)
    private Stop boardingStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_stop_id", nullable = false)
    private Stop destinationStop;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_record_id")
    private PassengerRecord passengerRecord;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "boarded_at")
    private LocalDateTime boardedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        requestedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Passenger getPassenger() { return passenger; }
    public void setPassenger(Passenger passenger) { this.passenger = passenger; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public Stop getBoardingStop() { return boardingStop; }
    public void setBoardingStop(Stop boardingStop) { this.boardingStop = boardingStop; }
    public Stop getDestinationStop() { return destinationStop; }
    public void setDestinationStop(Stop destinationStop) { this.destinationStop = destinationStop; }
    public PassengerRecord getPassengerRecord() { return passengerRecord; }
    public void setPassengerRecord(PassengerRecord passengerRecord) { this.passengerRecord = passengerRecord; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getBoardedAt() { return boardedAt; }
    public void setBoardedAt(LocalDateTime boardedAt) { this.boardedAt = boardedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
