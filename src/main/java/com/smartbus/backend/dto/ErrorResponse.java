package com.smartbus.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public class ErrorResponse {

    private boolean success;
    private String message;
    private String errorCode;
    private Map<String, String> details;
    private OffsetDateTime timestamp;

    public ErrorResponse() {
        this.success = false;
        this.timestamp = OffsetDateTime.now();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
