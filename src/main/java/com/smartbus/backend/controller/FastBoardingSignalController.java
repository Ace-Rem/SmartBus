package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.FastBoardingSignalRequest;
import com.smartbus.backend.dto.FastBoardingSignalAcceptRequest;
import com.smartbus.backend.dto.FastBoardingAcceptanceResponse;
import com.smartbus.backend.dto.FastBoardingSignalCancelRequest;
import com.smartbus.backend.dto.FastBoardingSignalResponse;
import com.smartbus.backend.service.FastBoardingSignalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fast-boarding-signals")
@Tag(name = "Fast Boarding Signals")
@SecurityRequirement(name = "bearerAuth")
public class FastBoardingSignalController {

    private final FastBoardingSignalService service;

    public FastBoardingSignalController(FastBoardingSignalService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Publish a lightweight boarding signal")
    public ResponseEntity<ApiResponse<FastBoardingSignalResponse>> publish(
            @Valid @RequestBody FastBoardingSignalRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(service.publish(request)));
    }

    @GetMapping
    @Operation(summary = "Poll lightweight boarding signals for a route")
    public ResponseEntity<ApiResponse<List<FastBoardingSignalResponse>>> poll(
            @RequestParam Long routeId,
            @RequestParam(required = false, defaultValue = "0") Long afterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.poll(routeId, afterId)));
    }

    @PostMapping("/{signalId}/accept")
    @Operation(summary = "Driver accepts a passenger fast boarding signal")
    public ResponseEntity<ApiResponse<FastBoardingAcceptanceResponse>> accept(
            @PathVariable Long signalId,
            @RequestBody FastBoardingSignalAcceptRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.accept(signalId, request)));
    }

    @PostMapping("/accept")
    @Operation(summary = "Driver accepts a passenger signal by identity")
    public ResponseEntity<ApiResponse<FastBoardingAcceptanceResponse>> acceptByIdentity(
            @RequestBody FastBoardingSignalAcceptRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.accept(null, request)));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Passenger cancels a fast boarding signal")
    public ResponseEntity<ApiResponse<FastBoardingSignalResponse>> cancel(
            @RequestBody FastBoardingSignalCancelRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.cancel(request)));
    }

    @GetMapping("/accepted")
    @Operation(summary = "Passenger polls driver acceptance")
    public ResponseEntity<ApiResponse<List<FastBoardingAcceptanceResponse>>> pollAccepted(
            @RequestParam(required = false, defaultValue = "0") Long afterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.pollAccepted(afterId)));
    }

    @GetMapping("/cancelled")
    @Operation(summary = "Driver polls cancelled fast boarding signals")
    public ResponseEntity<ApiResponse<List<FastBoardingSignalResponse>>> pollCancelled(
            @RequestParam Long routeId,
            @RequestParam(required = false, defaultValue = "0") Long afterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.pollCancelled(routeId, afterId)));
    }
}
