package com.smartbus.backend.service;

import com.smartbus.backend.dto.AiContextSyncRequest;
import com.smartbus.backend.dto.AiContextSyncResponse;
import java.util.Map;

public interface AiContextService {

    AiContextSyncResponse sync(AiContextSyncRequest request);

    Map<String, Object> loadForCurrentAccount(Long tripId);
}
