package com.smartbus.backend.dto;

public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresInMinutes;
    private DriverResponse driver;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresInMinutes() {
        return expiresInMinutes;
    }

    public void setExpiresInMinutes(Long expiresInMinutes) {
        this.expiresInMinutes = expiresInMinutes;
    }

    public DriverResponse getDriver() {
        return driver;
    }

    public void setDriver(DriverResponse driver) {
        this.driver = driver;
    }
}
