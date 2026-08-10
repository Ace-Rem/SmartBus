package com.smartbus.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class BootstrapResponse {

    private LocalDateTime serverTime;
    private List<RouteResponse> routes;
    private List<StopResponse> stops;
    private List<TripResponse> activeTrips;

    public LocalDateTime getServerTime() {
        return serverTime;
    }

    public void setServerTime(LocalDateTime serverTime) {
        this.serverTime = serverTime;
    }

    public List<RouteResponse> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteResponse> routes) {
        this.routes = routes;
    }

    public List<StopResponse> getStops() {
        return stops;
    }

    public void setStops(List<StopResponse> stops) {
        this.stops = stops;
    }

    public List<TripResponse> getActiveTrips() {
        return activeTrips;
    }

    public void setActiveTrips(List<TripResponse> activeTrips) {
        this.activeTrips = activeTrips;
    }
}
