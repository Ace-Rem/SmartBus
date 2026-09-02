package com.smartbus.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartbus.backend.dto.RouteResponse;
import com.smartbus.backend.dto.RoutePathPointResponse;
import com.smartbus.backend.dto.RoutePathResponse;
import com.smartbus.backend.dto.StopResponse;
import com.smartbus.backend.entity.Route;
import com.smartbus.backend.exception.BadRequestException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.mapper.RouteMapper;
import com.smartbus.backend.mapper.StopMapper;
import com.smartbus.backend.repository.RouteRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.service.RouteService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteMapper routeMapper;
    private final StopMapper stopMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RouteServiceImpl(
            RouteRepository routeRepository,
            StopRepository stopRepository,
            RouteMapper routeMapper,
            StopMapper stopMapper,
            ObjectMapper objectMapper
    ) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> listActiveRoutes() {
        return routeRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(routeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getById(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routeId));
        return routeMapper.toResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopResponse> listStopsByRoute(Long routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResourceNotFoundException("Route not found: " + routeId);
        }
        return stopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(routeId).stream()
                .map(stopMapper::toResponse)
                .toList();
    }

    @Override
    public RoutePathResponse routePath(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude
    ) {
        validateCoordinate(fromLatitude, "fromLatitude");
        validateCoordinate(fromLongitude, "fromLongitude");
        validateCoordinate(toLatitude, "toLatitude");
        validateCoordinate(toLongitude, "toLongitude");
        try {
            String coordinates = coordinate(fromLongitude) + "," + coordinate(fromLatitude)
                    + ";" + coordinate(toLongitude) + "," + coordinate(toLatitude);
            String url = "https://router.project-osrm.org/route/v1/driving/"
                    + coordinates
                    + "?overview=full&geometries=geojson";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResourceNotFoundException("No road path found for selected stops");
            }
            JsonNode coordinatesNode = objectMapper.readTree(response.body())
                    .path("routes")
                    .path(0)
                    .path("geometry")
                    .path("coordinates");
            if (!coordinatesNode.isArray() || coordinatesNode.isEmpty()) {
                throw new ResourceNotFoundException("No road path found for selected stops");
            }
            List<RoutePathPointResponse> points = new ArrayList<>();
            for (JsonNode point : coordinatesNode) {
                if (point.isArray() && point.size() >= 2) {
                    points.add(new RoutePathPointResponse(point.get(1).decimalValue(), point.get(0).decimalValue()));
                }
            }
            if (points.size() < 2) {
                throw new ResourceNotFoundException("No road path found for selected stops");
            }
            return new RoutePathResponse("OSRM", points);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResourceNotFoundException("No road path found for selected stops");
        }
    }

    private void validateCoordinate(BigDecimal value, String field) {
        if (value == null) {
            throw new BadRequestException(field + " is required");
        }
    }

    private String coordinate(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
