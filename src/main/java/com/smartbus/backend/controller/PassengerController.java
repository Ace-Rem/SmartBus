package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.PassengerLoginRequest;
import com.smartbus.backend.dto.PassengerLoginResponse;
import com.smartbus.backend.dto.PassengerRegisterRequest;
import com.smartbus.backend.dto.PassengerResponse;
import com.smartbus.backend.service.PassengerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passengers")
@Tag(name = "Passengers")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register passenger")
    public ResponseEntity<ApiResponse<PassengerLoginResponse>> register(
            @Valid @RequestBody PassengerRegisterRequest request
    ) {
        PassengerLoginResponse data = passengerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Passenger registered", data));
    }

    @PostMapping("/login")
    @Operation(summary = "Login passenger")
    public ResponseEntity<ApiResponse<PassengerLoginResponse>> login(
            @Valid @RequestBody PassengerLoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Passenger logged in", passengerService.login(request)));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Current passenger profile")
    public ResponseEntity<ApiResponse<PassengerResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success(passengerService.getMe()));
    }
}
