package com.smartbus.backend.service.impl;

import com.smartbus.backend.dto.CreatePassengerRecordRequest;
import com.smartbus.backend.dto.PassengerRecordResponse;
import com.smartbus.backend.entity.PassengerRecord;
import com.smartbus.backend.entity.Stop;
import com.smartbus.backend.entity.Trip;
import com.smartbus.backend.exception.BadRequestException;
import com.smartbus.backend.exception.ForbiddenException;
import com.smartbus.backend.exception.ResourceNotFoundException;
import com.smartbus.backend.mapper.PassengerRecordMapper;
import com.smartbus.backend.repository.PassengerRecordRepository;
import com.smartbus.backend.repository.StopRepository;
import com.smartbus.backend.repository.TripRepository;
import com.smartbus.backend.security.SecurityUtils;
import com.smartbus.backend.service.PassengerRecordService;
import com.smartbus.backend.util.PassengerGroupNoteBuilder;
import com.smartbus.backend.util.TripStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PassengerRecordServiceImpl implements PassengerRecordService {

    private final PassengerRecordRepository passengerRecordRepository;
    private final TripRepository tripRepository;
    private final StopRepository stopRepository;
    private final PassengerRecordMapper passengerRecordMapper;

    public PassengerRecordServiceImpl(
            PassengerRecordRepository passengerRecordRepository,
            TripRepository tripRepository,
            StopRepository stopRepository,
            PassengerRecordMapper passengerRecordMapper
    ) {
        this.passengerRecordRepository = passengerRecordRepository;
        this.tripRepository = tripRepository;
        this.stopRepository = stopRepository;
        this.passengerRecordMapper = passengerRecordMapper;
    }

    @Override
    @Transactional
    public PassengerRecordResponse create(CreatePassengerRecordRequest request) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        Trip trip = tripRepository.findByIdWithDetails(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + request.getTripId()));

        if (!trip.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Trip does not belong to current driver");
        }
        if (!TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new BadRequestException("Passenger records can only be added to an in-progress trip");
        }
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            PassengerRecord existing = passengerRecordRepository
                    .findByTripIdAndIdempotencyKey(trip.getId(), request.getIdempotencyKey())
                    .orElse(null);
            if (existing != null) {
                return passengerRecordMapper.toResponse(existing);
            }
        }

        // stopId = bến xuống (alighting). Một PassengerRecord = một nhóm, không quản lý cá nhân.
        Long destinationStopId = request.getDestinationStopId() != null
                ? request.getDestinationStopId()
                : request.getStopId();
        Stop alightingStop = null;
        if (destinationStopId != null) {
            alightingStop = stopRepository.findByIdAndRouteId(destinationStopId, trip.getRoute().getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Stop does not belong to the trip route: " + destinationStopId
                    ));
        }

        Stop boardingStop = trip.getCurrentStop();
        if (request.getBoardingStopId() != null) {
            boardingStop = stopRepository.findByIdAndRouteId(request.getBoardingStopId(), trip.getRoute().getId())
                    .orElseThrow(() -> new BadRequestException(
                            "Boarding stop does not belong to the trip route: " + request.getBoardingStopId()
                    ));
        }
        if (alightingStop != null && boardingStop != null
                && alightingStop.getStopOrder() != null
                && boardingStop.getStopOrder() != null
                && alightingStop.getStopOrder() <= boardingStop.getStopOrder()) {
            throw new BadRequestException(
                    "Alighting stop must be after the current boarding stop on the route"
            );
        }

        PassengerRecord record = new PassengerRecord();
        record.setTrip(trip);
        record.setStop(alightingStop);
        record.setBoardingStop(boardingStop);
        record.setPassengerCount(request.getPassengerCount());
        record.setNote(PassengerGroupNoteBuilder.build(boardingStop, alightingStop, request.getNote()));
        record.setSource(request.getSource());
        record.setIdempotencyKey(request.getIdempotencyKey());

        PassengerRecord saved = passengerRecordRepository.save(record);
        return passengerRecordMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerRecordResponse> listByTrip(Long tripId) {
        Long driverId = SecurityUtils.requireCurrentDriverId();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        if (!trip.getDriver().getId().equals(driverId)) {
            throw new ForbiddenException("Trip does not belong to current driver");
        }
        return passengerRecordRepository.findByTripIdOrderByRecordedAtAsc(tripId).stream()
                .map(passengerRecordMapper::toResponse)
                .toList();
    }
}
