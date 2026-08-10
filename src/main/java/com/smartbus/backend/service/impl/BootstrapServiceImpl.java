package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.BootstrapResponse;
import com.smartbus.backend.mapper.RouteMapper;
import com.smartbus.backend.mapper.StopMapper;
import com.smartbus.backend.mapper.TripMapper;
import com.smartbus.backend.repository.RouteRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.service.BootstrapService;
import com.smartbus.backend.util.TripStatus;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapServiceImpl implements BootstrapService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final TripRepository tripRepository;
    private final RouteMapper routeMapper;
    private final StopMapper stopMapper;
    private final TripMapper tripMapper;

    public BootstrapServiceImpl(
            RouteRepository routeRepository,
            StopRepository stopRepository,
            TripRepository tripRepository,
            RouteMapper routeMapper,
            StopMapper stopMapper,
            TripMapper tripMapper
    ) {
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.tripRepository = tripRepository;
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
        this.tripMapper = tripMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public BootstrapResponse bootstrap() {
        BootstrapResponse response = new BootstrapResponse();
        response.setServerTime(LocalDateTime.now());
        response.setRoutes(routeRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(routeMapper::toResponse)
                .toList());
        response.setStops(stopRepository.findByActiveTrue().stream()
                .map(stopMapper::toResponse)
                .toList());
        response.setActiveTrips(tripRepository.findByStatusOrderByStartedAtDesc(TripStatus.IN_PROGRESS).stream()
                .map(tripMapper::toResponse)
                .toList());
        return response;
    }
}
