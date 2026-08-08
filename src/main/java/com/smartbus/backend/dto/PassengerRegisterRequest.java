package com.smartbus.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class PassengerRegisterRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
