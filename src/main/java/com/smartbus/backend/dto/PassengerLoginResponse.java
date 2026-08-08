package com.smartbus.backend.dto;

public class PassengerLoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresInMinutes;
    private PassengerResponse passenger;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public Long getExpiresInMinutes() { return expiresInMinutes; }
    public void setExpiresInMinutes(Long expiresInMinutes) { this.expiresInMinutes = expiresInMinutes; }
    public PassengerResponse getPassenger() { return passenger; }
    public void setPassenger(PassengerResponse passenger) { this.passenger = passenger; }
}
