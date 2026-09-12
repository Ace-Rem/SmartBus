package com.smartbus.backend.dto;

public class AiContextSyncResponse {

    private boolean accepted;
    private String storedAt;

    public AiContextSyncResponse() {
    }

    public AiContextSyncResponse(boolean accepted, String storedAt) {
        this.accepted = accepted;
        this.storedAt = storedAt;
    }

    public boolean isAccepted() { return accepted; }
    public String getStoredAt() { return storedAt; }
}
