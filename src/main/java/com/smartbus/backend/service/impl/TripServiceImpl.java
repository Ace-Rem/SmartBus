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
import com.smartbus.backend.service.BoardingRequestService;
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
    private final BoardingRequestService boardingRequestService;

    public TripServiceImpl(
            TripRepository tripRepository,
            DriverRepository driverRepository,
            RouteRepository routeRepository,
            StopRepository stopRepository,
            PassengerRecordRepository passengerRecordRepository,
            TripMapper tripMapper,
            StopMapper stopMapper,
            GpsProperties gpsProperties,
            BoardingRequestService boardingRequestService
    ) {
        this.tripRepository = tripRepository;
        this.driverRepository = driverRepository;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.passengerRecordRepository = passengerRecordRepository;
        this.tripMapper = tripMapper;
        this.stopMapper = stopMapper;
        this.gpsProperties = gpsProperties;
        this.boardingRequestService = boardingRequestService;
    }

    @Override
    @Transactional
    public TripResponse startTrip(CreateTripRequest request) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        var currentTrip = tripRepository.findByDriverIdAndStatus(driverId, TripStatus.IN_PROGRESS);
        if (currentTrip.isPresent()) {
            return tripMapper.toResponse(currentTrip.get());
        }

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

        NearestStopResult nearest = findNearestStop(trip.getRoute().getId(), latitude, longitude);
        Stop nearestStop = nearest == null ? null : nearest.stop();
        Double nearestDistance = nearest == null ? null : nearest.distanceMeters();

        boolean withinThreshold = nearestStop != null
                && nearestDistance != null
                && nearestDistance <= gpsProperties.getStopProximityMeters();

        // Current stop for UI/API = nearest to vehicle; next = following stop on route.
        Stop currentStop = nearestStop;
        Stop nextStop = findNextStop(nearestStop);

        if (nearestStop != null) {
            trip.setCurrentStop(nearestStop);
        }

        int passengersAlighting = 0;
        int passengersAlightingAtNext = 0;
        if (withinThreshold && nearestStop != null) {
            passengersAlighting = sumAlighting(trip.getId(), nearestStop);
        }
        if (nextStop != null) {
            passengersAlightingAtNext = sumAlighting(trip.getId(), nextStop);
        }

        Trip saved = tripRepository.save(trip);
        if (withinThreshold && nearestStop != null) {
            boardingRequestService.completeArrivedRequestsForTrip(saved.getId());
        }

        UpdateLocationResponse response = new UpdateLocationResponse();
        response.setTrip(tripMapper.toResponse(saved));
        response.setNearestStop(stopMapper.toResponse(nearestStop));
        response.setNearestStopDistanceMeters(nearestDistance);
        response.setWithinThreshold(withinThreshold);
        response.setCurrentStop(stopMapper.toResponse(currentStop));
        response.setNextStop(stopMapper.toResponse(nextStop));
        response.setPassengersAlightingAtCurrentStop(passengersAlighting);
        response.setNotification(TripNotificationBuilder.build(
                withinThreshold,
                currentStop,
                nextStop,
                nearestStop,
                nearestDistance,
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
        Stop current = resolveCurrentStop(trip);
        if (current == null) {
            throw new ResourceNotFoundException("Current stop is not set for this trip");
        }
        return stopMapper.toResponse(current);
    }

    @Override
    @Transactional(readOnly = true)
    public StopResponse getNextStop() {
        Trip trip = requireCurrentTripEntity();
        Stop current = resolveCurrentStop(trip);
        if (current == null) {
            throw new ResourceNotFoundException("Current stop is not set; cannot determine next stop");
        }
        Stop nextStop = findNextStop(current);
        if (nextStop == null) {
            throw new ResourceNotFoundException("Trip is already at the last stop");
        }
        return stopMapper.toResponse(nextStop);
    }

    /**
     * Prefer nearest stop from last GPS; fall back to persisted currentStop.
     */
    private Stop resolveCurrentStop(Trip trip) {
        if (trip.getCurrentLatitude() != null && trip.getCurrentLongitude() != null) {
            NearestStopResult nearest = findNearestStop(
                    trip.getRoute().getId(),
                    trip.getCurrentLatitude(),
                    trip.getCurrentLongitude()
            );
            if (nearest != null && nearest.stop() != null) {
                return nearest.stop();
            }
        }
        return trip.getCurrentStop();
    }

    private NearestStopResult findNearestStop(Long routeId, BigDecimal latitude, BigDecimal longitude) {
        if (routeId == null || latitude == null || longitude == null) {
            return null;
        }
        List<Stop> stops = stopRepository.findByRouteIdAndActiveTrueOrderByStopOrderAsc(routeId);
        Stop nearestStop = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Stop stop : stops) {
            if (stop.getLatitude() == null || stop.getLongitude() == null) {
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
        if (nearestStop == null) {
            return null;
        }
        return new NearestStopResult(nearestStop, nearestDistance);
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

    private record NearestStopResult(Stop stop, double distanceMeters) {
    }
}
