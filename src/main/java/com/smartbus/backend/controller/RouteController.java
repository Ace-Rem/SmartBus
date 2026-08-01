package com.smartbus.backend.controller;

import com.smartbus.backend.dto.ApiResponse;
import com.smartbus.backend.dto.RouteResponse;
import com.smartbus.backend.dto.StopResponse;
import com.smartbus.backend.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/routes")
@Tag(name = "Routes")
@SecurityRequirement(name = "bearerAuth")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    @Operation(summary = "List active routes")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> listRoutes() {
        return ResponseEntity.ok(ApiResponse.success(routeService.listActiveRoutes()));
    }

    @GetMapping("/{routeId}")
    @Operation(summary = "Get route by id")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(ApiResponse.success(routeService.getById(routeId)));
    }

    @GetMapping("/{routeId}/stops")
    @Operation(summary = "List stops of a route")
    public ResponseEntity<ApiResponse<List<StopResponse>>> listStops(@PathVariable Long routeId) {
        return ResponseEntity.ok(ApiResponse.success(routeService.listStopsByRoute(routeId)));
    }
}
