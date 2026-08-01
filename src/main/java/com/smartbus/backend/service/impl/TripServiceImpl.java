package com.smartbus.backend.service.impl;

import com.smartbus.backend.config.GpsProperties;
import com.smartbus.backend.dto.CreateTripRequest;
import com.smartbus.backend.dto.StopResponse;
import com.smartbus.backend.dto.TripResponse;
import com.smartbus.backend.dto.UpdateLocationRequest;
import com.smartbus.backend.dto.UpdateLocationResponse;
import com.smartbus.backend.entity.Driver;
import com.smartbus.backend.entity.Route;
import com.smartbus.backend.entity.Stop;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.exception.BadRequestException;
import com.smartbus.backend.exception.ConflictException;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.mapper.StopMapper;
import com.smartbus.backend.mapper.TripMapper;
import com.smartbus.backend.repository.DriverRepository;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.RouteRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.TripService;
import com.smartbus.backend.util.GeoUtils;
import com.smartbus.backend.util.TripNotificationBuilder;
import com.smartbus.backend.util.TripStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final DriverRepository driverRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final PassengerRecordRepository passengerRecordRepository;
    private final TripMapper tripMapper;
    private final StopMapper stopMapper;
    private final GpsProperties gpsProperties;

    public TripServiceImpl(
            TripRepository tripRepository,
            DriverRepository driverRepository,
            RouteRepository routeRepository,
            StopRepository stopRepository,
            PassengerRecordRepository passengerRecordRepository,
            TripMapper tripMapper,
            StopMapper stopMapper,
            GpsProperties gpsProperties
    ) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.passengerRecordRepository = passengerRecordRepository;
        this.tripMapper = tripMapper;
        this.stopMapper = stopMapper;
        this.gpsProperties = gpsProperties;
    }

    @Override
    @Transactional
    public TripResponse startTrip(CreateTripRequest request) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        tripRepository.findByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS).ifPresent(trip -> {
            throw new ConflictException("Driver already has an in-progress trip");
        });

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + driverId));
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + request.getRouteId()));

        if (!Boolean.TRUE.equals(route.getActive())) {
            throw new BadRequestException("Route is inactive");
        }

        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setRoute(route);
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartedAt(LocalDateTime.now());
        Trip saved = tripRepository.save(trip);
        return tripMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TripResponse endCurrentTrip() {
        Trip trip = requireCurrentTripEntity();
        trip.setStatus(TripStatus.COMPLETED);
        trip.setEndedAt(LocalDateTime.now());
        return tripMapper.toResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getCurrentTrip() {
        return tripMapper.toResponse(requireCurrentTripEntity());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getHistory() {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        return tripRepository.findByDriverIdOrderByStartedAtDesc(driverId).stream()
                .map(tripMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getById(Long tripId) {
        Trip trip = requireOwnedTrip(tripId);
        return tripMapper.toResponse(trip);
    }

    @Override
    @Transactional
    public UpdateLocationResponse updateCurrentLocation(UpdateLocationRequest request) {
        Trip trip = requireCurrentTripEntity();
        BigDecimal latitude = request.getLatitude();
        BigDecimal longitude = request.getLongitude();

        trip.setCurrentLatitude(latitude);
        trip.setCurrentLongitude(longitude);

        List<Stop> stops = stopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(trip.getRoute().getId());
        Stop nearestStop = null;
        double nearestDistance = Double.MAX_VALUE;
        Integer currentOrder = trip.getCurrentStop() != null ? trip.getCurrentStop().getStopOrder() : null;
        for (Stop stop : stops) {
            // Only allow advancing along the route (or any stop if none set yet).
            if (currentOrder != null
                    && stop.getStopOrder() != null
                    && stop.getStopOrder() < currentOrder) {
                continue;
            }
            double distance = GeoUtils.distanceMeters(
                    latitude,
                    longitude,
                    stop.getLatitude(),
                    stop.getLongitude()
            );
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestStop = stop;
            }
        }

        boolean withinThreshold = nearestStop != null
                && nearestDistance <= gpsProperties.getStopProximityMeters();

        Stop currentStop = trip.getCurrentStop();
        Stop nextStop = null;
        int passengersAlighting = 0;
        int passengersAlightingAtNext = 0;

        if (withinThreshold) {
            trip.setCurrentStop(nearestStop);
            currentStop = nearestStop;
            nextStop = findNextStop(nearestStop);
            passengersAlighting = sumAlighting(trip.getId(), nearestStop);
        } else if (currentStop != null) {
            nextStop = findNextStop(currentStop);
            passengersAlighting = sumAlighting(trip.getId(), currentStop);
        }

        if (nextStop != null) {
            passengersAlightingAtNext = sumAlighting(trip.getId(), nextStop);
        }

        Trip saved = tripRepository.save(trip);

        UpdateLocationResponse response = new UpdateLocationResponse();
        response.setTrip(tripMapper.toResponse(saved));
        response.setNearestStop(stopMapper.toResponse(nearestStop));
        response.setNearestStopDistanceMeters(nearestStop == null ? null : nearestDistance);
        response.setWithinThreshold(withinThreshold);
        response.setCurrentStop(stopMapper.toResponse(currentStop));
        response.setNextStop(stopMapper.toResponse(nextStop));
        response.setPassengersAlightingAtCurrentStop(passengersAlighting);
        response.setNotification(TripNotificationBuilder.build(
                withinThreshold,
                currentStop,
                nextStop,
                nearestStop,
                nearestStop == null ? null : nearestDistance,
                passengersAlighting,
                passengersAlightingAtNext
        ));
        return response;
    }

    private int sumAlighting(Long tripId, Stop stop) {
        if (stop == null || stop.getId() == null) {
            return 0;
        }
        return passengerRecordRepository.sumPassengerCountByTripIdAndStopId(tripId, stop.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public StopResponse getCurrentStop() {
        Trip trip = requireCurrentTripEntity();
        if (trip.getCurrentStop() == null) {
            throw new ResourceNotFoundException("Current stop is not set for this trip");
        }
        return stopMapper.toResponse(trip.getCurrentStop());
    }

    @Override
    @Transactional(readOnly = true)
    public StopResponse getNextStop() {
        Trip trip = requireCurrentTripEntity();
        if (trip.getCurrentStop() == null) {
            throw new ResourceNotFoundException("Current stop is not set; cannot determine next stop");
        }
        Stop nextStop = findNextStop(trip.getCurrentStop());
        if (nextStop == null) {
            throw new ResourceNotFoundException("Trip is already at the last stop");
        }
        return stopMapper.toResponse(nextStop);
    }

    private Stop findNextStop(Stop currentStop) {
        if (currentStop == null || currentStop.getRoute() == null || currentStop.getStopOrder() == null) {
            return null;
        }
        return stopRepository
                .findFirstByRouteIdAndActiveTrueAndStopOrderGreaterThanOrderByStopOrderAsc(
                        currentStop.getRoute().getId(),
                        currentStop.getStopOrder()
                )
                .orElse(null);
    }

    private Trip requireCurrentTripEntity() {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        return tripRepository.findCurrentTripWithDetails(driverId, TripStatus.IN_PROGRESS)
                .orElseThrow(() -> new ResourceNotFoundException("No in-progress trip for current driver"));
    }

    private Trip requireOwnedTrip(Long tripId) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Trip does not belong to current driver");
        }
        return trip;
    }
}
