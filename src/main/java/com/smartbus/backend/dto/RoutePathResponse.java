package com.smartbus.backend.dto;

import java.util.List;

public class RoutePathResponse {

    private String provider;
    private List<RoutePathPointResponse> points;

    public RoutePathResponse() {
    }

    public RoutePathResponse(String provider, List<RoutePathPointResponse> points) {
        this.provider = provider;
        this.points = points;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<RoutePathPointResponse> getPoints() {
        return points;
    }

    public void setPoints(List<RoutePathPointResponse> points) {
        this.points = points;
    }
}
