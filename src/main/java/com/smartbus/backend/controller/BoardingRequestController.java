package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.BoardingRequestResponse;
import com.smartbus.backend.dto.CreateBoardingRequest;
import com.smartbus.backend.dto.PassengerTripTrackingResponse;
import com.smartbus.backend.dto.TripResponse;
import com.smartbus.backend.service.BoardingRequestService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/boarding-requests")
@Tag(name = "Boarding Requests")
@SecurityRequirement(name = "bearerAuth")
public class BoardingRequestController {

    private final BoardingRequestService boardingRequestService;

    public BoardingRequestController(BoardingRequestService boardingRequestService) {
        this.boardingRequestService = boardingRequestService;
    }

    @GetMapping("/active-trips")
    @Operation(summary = "Find active trips matching passenger stop choice")
    public ResponseEntity<ApiResponse<List<TripResponse>>> activeTrips(
            @RequestParam Long routeId,
            @RequestParam Long boardingStopId,
            @RequestParam Long destinationStopId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                boardingRequestService.findActiveTrips(routeId, boardingStopId, destinationStopId)
        ));
    }

    @PostMapping
    @Operation(summary = "Create boarding request")
    public ResponseEntity<ApiResponse<BoardingRequestResponse>> create(
            @Valid @RequestBody CreateBoardingRequest request
    ) {
        BoardingRequestResponse data = boardingRequestService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Boarding request created", data));
    }

    @GetMapping("/mine")
    @Operation(summary = "List current passenger boarding requests")
    public ResponseEntity<ApiResponse<List<BoardingRequestResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.success(boardingRequestService.listMine()));
    }

    @GetMapping
    @Operation(summary = "List boarding requests by trip for driver")
    public ResponseEntity<ApiResponse<List<BoardingRequestResponse>>> listByTrip(@RequestParam Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(boardingRequestService.listByTrip(tripId)));
    }

    @PostMapping("/{id}/confirm-boarding")
    @Operation(summary = "Driver confirms passenger boarded")
    public ResponseEntity<ApiResponse<BoardingRequestResponse>> confirmBoarding(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Boarding confirmed",
                boardingRequestService.confirmBoarding(id)
        ));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Passenger cancels boarding request")
    public ResponseEntity<ApiResponse<BoardingRequestResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Boarding request cancelled",
                boardingRequestService.cancel(id)
        ));
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "Passenger tracks boarded trip")
    public ResponseEntity<ApiResponse<PassengerTripTrackingResponse>> track(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(boardingRequestService.track(id)));
    }
}
