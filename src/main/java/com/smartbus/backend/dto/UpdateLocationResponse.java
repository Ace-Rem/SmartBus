package com.smartbus.backend.dto;

public class UpdateLocationResponse {

    private TripResponse trip;
    private StopResponse nearestStop;
    private Double nearestStopDistanceMeters;
    private boolean withinThreshold;
    private StopResponse currentStop;
    private StopResponse nextStop;
    private int passengersAlightingAtCurrentStop;
    private String notification;

    public TripResponse getTrip() {
        return trip;
    }

    public void setTrip(TripResponse trip) {
        this.trip = trip;
    }

    public StopResponse getNearestStop() {
        return nearestStop;
    }

    public void setNearestStop(StopResponse nearestStop) {
        this.nearestStop = nearestStop;
    }

    public Double getNearestStopDistanceMeters() {
        return nearestStopDistanceMeters;
    }

    public void setNearestStopDistanceMeters(Double nearestStopDistanceMeters) {
        this.nearestStopDistanceMeters = nearestStopDistanceMeters;
    }

    public boolean isWithinThreshold() {
        return withinThreshold;
    }

    public void setWithinThreshold(boolean withinThreshold) {
        this.withinThreshold = withinThreshold;
    }

    public StopResponse getCurrentStop() {
        return currentStop;
    }

    public void setCurrentStop(StopResponse currentStop) {
        this.currentStop = currentStop;
    }

    public StopResponse getNextStop() {
        return nextStop;
    }

    public void setNextStop(StopResponse nextStop) {
        this.nextStop = nextStop;
    }

    public int getPassengersAlightingAtCurrentStop() {
        return passengersAlightingAtCurrentStop;
    }

    public void setPassengersAlightingAtCurrentStop(int passengersAlightingAtCurrentStop) {
        this.passengersAlightingAtCurrentStop = passengersAlightingAtCurrentStop;
    }

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }
}
