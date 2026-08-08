package com.smartbus.backend.dto;

public class PassengerTripTrackingResponse {

    private BoardingRequestResponse boardingRequest;
    private RouteResponse route;
    private StopResponse currentStop;
    private StopResponse nextStop;
    private Integer remainingStops;
    private Integer etaMinutes;
    private Integer progressPercent;
    private String notification;

    public BoardingRequestResponse getBoardingRequest() { return boardingRequest; }
    public void setBoardingRequest(BoardingRequestResponse boardingRequest) { this.boardingRequest = boardingRequest; }
    public RouteResponse getRoute() { return route; }
    public void setRoute(RouteResponse route) { this.route = route; }
    public StopResponse getCurrentStop() { return currentStop; }
    public void setCurrentStop(StopResponse currentStop) { this.currentStop = currentStop; }
    public StopResponse getNextStop() { return nextStop; }
    public void setNextStop(StopResponse nextStop) { this.nextStop = nextStop; }
    public Integer getRemainingStops() { return remainingStops; }
    public void setRemainingStops(Integer remainingStops) { this.remainingStops = remainingStops; }
    public Integer getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(Integer etaMinutes) { this.etaMinutes = etaMinutes; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public String getNotification() { return notification; }
    public void setNotification(String notification) { this.notification = notification; }
}
