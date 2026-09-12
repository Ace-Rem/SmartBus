package com.smartbus.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Latest local snapshot uploaded by one authenticated app for one trip. */
@Entity
@Table(name = "ai_client_contexts", uniqueConstraints = @UniqueConstraint(
        name = "uk_ai_client_context_owner_trip",
        columnNames = {"owner_type", "owner_id", "client_key"}
))
public class AiClientContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_type", nullable = false, length = 20)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "client_key", nullable = false, length = 160)
    private String clientKey;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "context_json", nullable = false, columnDefinition = "TEXT")
    private String contextJson;

    @Column(name = "client_version")
    private Long clientVersion;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getClientKey() { return clientKey; }
    public void setClientKey(String clientKey) { this.clientKey = clientKey; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public Long getClientVersion() { return clientVersion; }
    public void setClientVersion(Long clientVersion) { this.clientVersion = clientVersion; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
