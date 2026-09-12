package com.smartbus.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbus.backend.dto.AiContextSyncRequest;
import com.smartbus.backend.dto.AiContextSyncResponse;
import com.smartbus.backend.entity.AiClientContext;
import com.smartbus.backend.exception.BadRequestException;
import com.smartbus.backend.exception.UnauthorizedException;
import com.smartbus.backend.repository.AiClientContextRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.AiContextService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiContextServiceImpl implements AiContextService {

    private static final int MAX_CONTEXT_BYTES = 128 * 1024;
    private final AiClientContextRepository repository;
    private final ObjectMapper objectMapper;

    public AiContextServiceImpl(AiClientContextRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AiContextSyncResponse sync(AiContextSyncRequest request) {
        Owner owner = currentOwner();
        String json;
        try {
            json = objectMapper.writeValueAsString(request.getContext());
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("AI context is not valid JSON");
        }
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CONTEXT_BYTES) {
            throw new BadRequestException("AI context is too large");
        }

        String clientKey = String.valueOf(request.getTripId());
        AiClientContext row = repository.findByOwnerTypeAndOwnerIdAndClientKey(
                        owner.type(), owner.id(), clientKey)
                .orElseGet(AiClientContext::new);
        if (row.getId() != null
                && row.getClientVersion() != null
                && request.getClientVersion() != null
                && request.getClientVersion() < row.getClientVersion()) {
            return new AiContextSyncResponse(false,
                    row.getUpdatedAt() == null ? null : row.getUpdatedAt().toString());
        }
        row.setOwnerType(owner.type());
        row.setOwnerId(owner.id());
        row.setClientKey(clientKey);
        row.setTripId(request.getTripId());
        row.setContextJson(json);
        row.setClientVersion(request.getClientVersion());
        AiClientContext saved = repository.save(row);
        return new AiContextSyncResponse(true,
                saved.getUpdatedAt() == null ? LocalDateTime.now().toString() : saved.getUpdatedAt().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> loadForCurrentAccount(Long tripId) {
        if (tripId == null) return Collections.emptyMap();
        Owner owner = currentOwnerOrNull();
        if (owner == null) return Collections.emptyMap();
        return repository.findByOwnerTypeAndOwnerIdAndClientKey(
                        owner.type(), owner.id(), String.valueOf(tripId))
                .map(AiClientContext::getContextJson)
                .map(this::parse)
                .orElseGet(Collections::emptyMap);
    }

    private Map<String, Object> parse(String json) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() { });
            return parsed == null ? Collections.emptyMap() : new LinkedHashMap<>(parsed);
        } catch (Exception exception) {
            return Collections.emptyMap();
        }
    }

    private Owner currentOwner() {
        Owner owner = currentOwnerOrNull();
        if (owner == null) throw new UnauthorizedException("AI context authentication required");
        return owner;
    }

    private Owner currentOwnerOrNull() {
        Long driverId = SecurityUtils.currentDriverIdOrNull();
        if (driverId != null) return new Owner("DRIVER", driverId);
        Long passengerId = SecurityUtils.currentPassengerIdOrNull();
        return passengerId == null ? null : new Owner("PASSENGER", passengerId);
    }

    private record Owner(String type, Long id) { }
}
