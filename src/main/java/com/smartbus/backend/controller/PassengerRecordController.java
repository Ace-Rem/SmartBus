package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.CreatePassengerRecordRequest;
import com.smartbus.backend.dto.PassengerRecordResponse;
import com.smartbus.backend.service.PassengerRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passenger-records")
@Tag(name = "Passenger Records")
@SecurityRequirement(name = "bearerAuth")
public class PassengerRecordController {

    private final PassengerRecordService passengerRecordService;

    public PassengerRecordController(PassengerRecordService passengerRecordService) {
        this.passengerRecordService = passengerRecordService;
    }

    @PostMapping
    @Operation(summary = "Create passenger record")
    public ResponseEntity<ApiResponse<PassengerRecordResponse>> create(
            @Valid @RequestBody CreatePassengerRecordRequest request
    ) {
        PassengerRecordResponse data = passengerRecordService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Passenger record created", data));
    }

    @GetMapping
    @Operation(summary = "List passenger records by trip")
    public ResponseEntity<ApiResponse<List<PassengerRecordResponse>>> listByTrip(
            @RequestParam Long tripId
    ) {
        return ResponseEntity.ok(ApiResponse.success(passengerRecordService.listByTrip(tripId)));
    }
}
