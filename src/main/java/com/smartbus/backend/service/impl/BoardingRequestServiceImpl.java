package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.BoardingRequestResponse;
import com.smartbus.backend.dto.CreateBoardingRequest;
import com.smartbus.backend.dto.PassengerTripTrackingResponse;
import com.smartbus.backend.dto.TripResponse;
import com.smartbus.backend.entity.BoardingRequest;
import com.smartbus.backend.entity.Passenger;
import com.smartbus.backend.entity.PassengerRecord;
import com.smartbus.backend.entity.Stop;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.exception.BadRequestException;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.mapper.BoardingRequestMapper;
import com.smartbus.backend.mapper.RouteMapper;
import com.smartbus.backend.mapper.StopMapper;
import com.smartbus.backend.mapper.TripMapper;
import com.smartbus.backend.repository.BoardingRequestRepository;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.PassengerRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.BoardingRequestService;
import com.smartbus.backend.util.BoardingRequestStatus;
import com.smartbus.backend.util.PassengerGroupNoteBuilder;
import com.smartbus.backend.util.TripStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardingRequestServiceImpl implements BoardingRequestService {

    private final BoardingRequestRepository boardingRequestRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final StopRepository stopRepository;
    private final PassengerRecordRepository passengerRecordRepository;
    private final BoardingRequestMapper boardingRequestMapper;
    private final TripMapper tripMapper;
    private final RouteMapper routeMapper;
    private final StopMapper stopMapper;

    public BoardingRequestServiceImpl(
            BoardingRequestRepository boardingRequestRepository,
            PassengerRepository passengerRepository,
            TripRepository tripRepository,
            StopRepository stopRepository,
            PassengerRecordRepository passengerRecordRepository,
            BoardingRequestMapper boardingRequestMapper,
            TripMapper tripMapper,
            RouteMapper routeMapper,
            StopMapper stopMapper
    ) {
        this.boardingRequestRepository = boardingRequestRepository;
        this.passengerRepository = passengerRepository;
        this.tripRepository = tripRepository;
        this.stopRepository = stopRepository;
        this.passengerRecordRepository = passengerRecordRepository;
        this.boardingRequestMapper = boardingRequestMapper;
        this.tripMapper = tripMapper;
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> findActiveTrips(Long routeId, Long boardingStopId, Long destinationStopId) {
        SecurityUtils.requireCurrentPassengerId();
        validateStopOrder(routeId, boardingStopId, destinationStopId);
        return tripRepository.findByRouteIdAndStatusOrderByStartedAtDesc(routeId, TripStatus.IN_PROGRESS).stream()
                .map(tripMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BoardingRequestResponse create(CreateBoardingRequest request) {
        Long passengerId = SecurityUtils.requireCurrentPassengerId();
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found: " + passengerId));
        Trip trip = tripRepository.findByIdWithDetails(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + request.getTripId()));
        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new BadRequestException("Boarding request requires an in-progress trip");
        }
        Stop[] stops = validateStopOrder(
                trip.getRoute().getId(),
                request.getBoardingStopId(),
                request.getDestinationStopId()
        );

        BoardingRequest boardingRequest = new BoardingRequest();
        boardingRequest.setPassenger(passenger);
        boardingRequest.setTrip(trip);
        boardingRequest.setBoardingStop(stops[0]);
        boardingRequest.setDestinationStop(stops[1]);
        boardingRequest.setStatus(BoardingRequestStatus.PENDING);
        boardingRequest.setNote(request.getNote());
        return boardingRequestMapper.toResponse(boardingRequestRepository.save(boardingRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardingRequestResponse> listMine() {
        Long passengerId = SecurityUtils.requireCurrentPassengerId();
        return boardingRequestRepository.findByPassengerIdOrderByRequestedAtDesc(passengerId).stream()
                .map(boardingRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardingRequestResponse> listByTrip(Long tripId) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Trip does not belong to current driver");
        }
        return boardingRequestRepository.findByTripIdOrderByRequestedAtAsc(tripId).stream()
                .map(boardingRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BoardingRequestResponse confirmBoarding(Long id) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        BoardingRequest request = requireRequest(id);
        Trip trip = request.getTrip();
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Trip does not belong to current driver");
        }
        if (!BoardingRequestStatus.PENDING.equals(request.getStatus())
                && !BoardingRequestStatus.CONFIRMED.equals(request.getStatus())) {
            throw new BadRequestException("Boarding request cannot be boarded from status " + request.getStatus());
        }

        PassengerRecord record = new PassengerRecord();
        record.setTrip(trip);
        record.setStop(request.getDestinationStop());
        record.setPassengerCount(1);
        record.setNote(PassengerGroupNoteBuilder.build(
                request.getBoardingStop(),
                request.getDestinationStop(),
                request.getNote()
        ));
        PassengerRecord savedRecord = passengerRecordRepository.save(record);

        LocalDateTime now = LocalDateTime.now();
        request.setStatus(BoardingRequestStatus.BOARDED);
        request.setConfirmedAt(now);
        request.setBoardedAt(now);
        request.setPassengerRecord(savedRecord);
        return boardingRequestMapper.toResponse(boardingRequestRepository.save(request));
    }

    @Override
    @Transactional
    public BoardingRequestResponse cancel(Long id) {
        Long passengerId = SecurityUtils.requireCurrentPassengerId();
        BoardingRequest request = requireRequest(id);
        if (!request.getPassenger().getId().equals(passengerId)) {
            throw new ForbiddenException("Boarding request does not belong to current passenger");
        }
        if (BoardingRequestStatus.BOARDED.equals(request.getStatus())
                || BoardingRequestStatus.COMPLETED.equals(request.getStatus())) {
            throw new BadRequestException("Boarding request can no longer be cancelled");
        }
        request.setStatus(BoardingRequestStatus.CANCELLED);
        request.setCancelledAt(LocalDateTime.now());
        return boardingRequestMapper.toResponse(boardingRequestRepository.save(request));
    }

    @Override
    @Transactional
    public PassengerTripTrackingResponse track(Long id) {
        Long passengerId = SecurityUtils.requireCurrentPassengerId();
        BoardingRequest request = requireRequest(id);
        if (!request.getPassenger().getId().equals(passengerId)) {
            throw new ForbiddenException("Boarding request does not belong to current passenger");
        }

        Stop currentStop = request.getTrip().getCurrentStop();
        Stop nextStop = null;
        int remainingStops = 0;
        int etaMinutes = 0;
        int progressPercent = 0;
        String notification = "Chuyến xe đang được theo dõi.";

        if (currentStop != null && currentStop.getStopOrder() != null) {
            nextStop = stopRepository
                    .findFirstByRouteIdAndActiveTrueAndStopOrderGreaterThanOrderByStopOrderAsc(
                            request.getTrip().getRoute().getId(),
                            currentStop.getStopOrder()
                    )
                    .orElse(null);
            remainingStops = Math.max(0, request.getDestinationStop().getStopOrder() - currentStop.getStopOrder());
            etaMinutes = remainingStops * 3;
            int totalToDestination = Math.max(
                    1,
                    request.getDestinationStop().getStopOrder() - request.getBoardingStop().getStopOrder()
            );
            int completed = Math.max(0, currentStop.getStopOrder() - request.getBoardingStop().getStopOrder());
            progressPercent = Math.min(100, Math.max(0, (completed * 100) / totalToDestination));
            if (remainingStops == 0) {
                notification = "Bạn đã đến bến xuống.";
                markCompletedIfNeeded(request);
            } else if (remainingStops <= 1) {
                notification = "Sắp đến bến xuống, vui lòng chuẩn bị.";
            }
        }

        PassengerTripTrackingResponse response = new PassengerTripTrackingResponse();
        response.setBoardingRequest(boardingRequestMapper.toResponse(request));
        response.setRoute(routeMapper.toResponse(request.getTrip().getRoute()));
        response.setCurrentStop(stopMapper.toResponse(currentStop));
        response.setNextStop(stopMapper.toResponse(nextStop));
        response.setRemainingStops(remainingStops);
        response.setEtaMinutes(etaMinutes);
        response.setProgressPercent(progressPercent);
        response.setNotification(notification);
        return response;
    }

    @Override
    @Transactional
    public List<BoardingRequestResponse> completeArrivedRequestsForTrip(Long tripId) {
        return boardingRequestRepository.findByTripIdOrderByRequestedAtAsc(tripId).stream()
                .map(request -> {
                    Stop currentStop = request.getTrip().getCurrentStop();
                    if (currentStop != null
                            && currentStop.getStopOrder() != null
                            && request.getDestinationStop() != null
                            && request.getDestinationStop().getStopOrder() != null
                            && currentStop.getStopOrder() >= request.getDestinationStop().getStopOrder()) {
                        markCompletedIfNeeded(request);
                    }
                    return boardingRequestMapper.toResponse(request);
                })
                .toList();
    }

    private void markCompletedIfNeeded(BoardingRequest request) {
        if (BoardingRequestStatus.BOARDED.equals(request.getStatus())) {
            request.setStatus(BoardingRequestStatus.COMPLETED);
            request.setCompletedAt(LocalDateTime.now());
            boardingRequestRepository.save(request);
        }
    }

    private BoardingRequest requireRequest(Long id) {
        return boardingRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boarding request not found: " + id));
    }

    private Stop[] validateStopOrder(Long routeId, Long boardingStopId, Long destinationStopId) {
        Stop boardingStop = stopRepository.findByIdAndRouteId(boardingStopId, routeId)
                .orElseThrow(() -> new BadRequestException("Boarding stop does not belong to route"));
        Stop destinationStop = stopRepository.findByIdAndRouteId(destinationStopId, routeId)
                .orElseThrow(() -> new BadRequestException("Destination stop does not belong to route"));
        if (destinationStop.getStopOrder() <= boardingStop.getStopOrder()) {
            throw new BadRequestException("Destination stop must be after boarding stop");
        }
        return new Stop[] { boardingStop, destinationStop };
    }
}
