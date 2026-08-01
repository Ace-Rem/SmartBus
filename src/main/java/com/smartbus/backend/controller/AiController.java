package com.smartbus.backend.controller;

import com.smartbus.backend.dto.AiAssistantRequest;
import com.smartbus.backend.dto.AiAssistantResponse;
import com.smartbus.backend.dto.AiSummaryRequest;
import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI Assistant")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    @Operation(summary = "AI chat for a trip")
    public ResponseEntity<ApiResponse<AiAssistantResponse>> chat(
            @Valid @RequestBody AiAssistantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiAssistantService.chat(request)));
    }

    @PostMapping("/summary")
    @Operation(summary = "AI summary for a trip")
    public ResponseEntity<ApiResponse<AiAssistantResponse>> summary(
            @Valid @RequestBody AiSummaryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(aiAssistantService.summarize(request)));
    }
}
