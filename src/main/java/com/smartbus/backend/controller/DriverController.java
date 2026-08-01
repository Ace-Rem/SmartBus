package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.DriverResponse;
import com.smartbus.backend.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/drivers")
@Tag(name = "Drivers")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current driver profile")
    public ResponseEntity<ApiResponse<DriverResponse>> getCurrentDriver() {
        return ResponseEntity.ok(ApiResponse.success(driverService.getCurrentDriver()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get driver by id")
    public ResponseEntity<ApiResponse<DriverResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(driverService.getById(id)));
    }
}
