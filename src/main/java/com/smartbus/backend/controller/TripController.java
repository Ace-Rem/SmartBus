package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.CreateTripRequest;
import com.smartbus.backend.dto.StopResponse;
import com.smartbus.backend.dto.TripResponse;
import com.smartbus.backend.dto.UpdateLocationRequest;
import com.smartbus.backend.dto.UpdateLocationResponse;
import com.smartbus.backend.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips")
@Tag(name = "Trips")
@SecurityRequirement(name = "bearerAuth")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @Operation(summary = "Start a new trip")
    public ResponseEntity<ApiResponse<TripResponse>> startTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse data = tripService.startTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Trip started", data));
    }

    @GetMapping("/current")
    @Operation(summary = "Get current in-progress trip")
    public ResponseEntity<ApiResponse<TripResponse>> getCurrentTrip() {
        return ResponseEntity.ok(ApiResponse.success(tripService.getCurrentTrip()));
    }

    @PostMapping("/current/end")
    @Operation(summary = "End current trip")
    public ResponseEntity<ApiResponse<TripResponse>> endCurrentTrip() {
        return ResponseEntity.ok(ApiResponse.success("Trip ended", tripService.endCurrentTrip()));
    }

    @PostMapping("/current/location")
    @Operation(summary = "Update GPS location for current trip")
    public ResponseEntity<ApiResponse<UpdateLocationResponse>> updateLocation(
            @Valid @RequestBody UpdateLocationRequest request
    ) {
        UpdateLocationResponse data = tripService.updateCurrentLocation(request);
        String message = data.getNotification() != null ? data.getNotification() : "Location updated";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    @GetMapping("/current/current-stop")
    @Operation(summary = "Get current stop of current trip")
    public ResponseEntity<ApiResponse<StopResponse>> getCurrentStop() {
        return ResponseEntity.ok(ApiResponse.success(tripService.getCurrentStop()));
    }

    @GetMapping("/current/next-stop")
    @Operation(summary = "Get next stop of current trip")
    public ResponseEntity<ApiResponse<StopResponse>> getNextStop() {
        return ResponseEntity.ok(ApiResponse.success(tripService.getNextStop()));
    }

    @GetMapping("/history")
    @Operation(summary = "Get trip history for current driver")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.success(tripService.getHistory()));
    }

    @GetMapping("/{tripId}")
    @Operation(summary = "Get trip by id")
    public ResponseEntity<ApiResponse<TripResponse>> getById(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(tripService.getById(tripId)));
    }
}
