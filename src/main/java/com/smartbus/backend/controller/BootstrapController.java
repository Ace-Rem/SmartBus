package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.BootstrapResponse;
import com.smartbus.backend.service.BootstrapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
@Tag(name = "Sync")
@SecurityRequirement(name = "bearerAuth")
public class BootstrapController {

    private final BootstrapService bootstrapService;

    public BootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/bootstrap")
    @Operation(summary = "Bootstrap cacheable app data for offline-first clients")
    public ResponseEntity<ApiResponse<BootstrapResponse>> bootstrap() {
        return ResponseEntity.ok(ApiResponse.success(bootstrapService.bootstrap()));
    }
}
