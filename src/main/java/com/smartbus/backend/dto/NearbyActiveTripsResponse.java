package com.smartbus.backend.dto;

import java.util.List;

public class NearbyActiveTripsResponse {

    private StopResponse boardingStop;
    private StopResponse suggestedDestinationStop;
    private RouteResponse route;
    private Double distanceMeters;
    private List<TripResponse> trips;

    public StopResponse getBoardingStop() {
        return boardingStop;
    }

    public void setBoardingStop(StopResponse boardingStop) {
        this.boardingStop = boardingStop;
    }

    public StopResponse getSuggestedDestinationStop() {
        return suggestedDestinationStop;
    }

    public void setSuggestedDestinationStop(StopResponse suggestedDestinationStop) {
        this.suggestedDestinationStop = suggestedDestinationStop;
    }

    public RouteResponse getRoute() {
        return route;
    }

    public void setRoute(RouteResponse route) {
        this.route = route;
    }

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public List<TripResponse> getTrips() {
        return trips;
    }

    public void setTrips(List<TripResponse> trips) {
        this.trips = trips;
    }
}
