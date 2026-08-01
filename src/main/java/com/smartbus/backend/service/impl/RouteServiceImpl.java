package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.RouteResponse;
import com.smartbus.backend.dto.StopResponse;
import com.smartbus.backend.entity.Route;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.mapper.RouteMapper;
import com.smartbus.backend.mapper.StopMapper;
import com.smartbus.backend.repository.RouteRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.service.RouteService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteMapper routeMapper;
    private final StopMapper stopMapper;

    public RouteServiceImpl(
            RouteRepository routeRepository,
            StopRepository stopRepository,
            RouteMapper routeMapper,
            StopMapper stopMapper
    ) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
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
}
