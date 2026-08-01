package com.smartbus.backend.service;

import com.smartbus.backend.dto.AiAssistantRequest;
import com.smartbus.backend.dto.AiAssistantResponse;
import com.smartbus.backend.dto.AiSummaryRequest;

public interface AiAssistantService {

    AiAssistantResponse chat(AiAssistantRequest request);

    AiAssistantResponse summarize(AiSummaryRequest request);
}
