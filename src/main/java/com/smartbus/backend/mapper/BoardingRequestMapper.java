package com.smartbus.backend.mapper;

import com.smartbus.backend.dto.BoardingRequestResponse;
import com.smartbus.backend.entity.BoardingRequest;
import org.springframework.stereotype.Component;

@Component
public class BoardingRequestMapper {

    private final PassengerMapper passengerMapper;
    private final TripMapper tripMapper;
    private final StopMapper stopMapper;

    public BoardingRequestMapper(
            PassengerMapper passengerMapper,
            TripMapper tripMapper,
            StopMapper stopMapper
    ) {
        this.passengerMapper = passengerMapper;
        this.tripMapper = tripMapper;
        this.stopMapper = stopMapper;
    }

    public BoardingRequestResponse toResponse(BoardingRequest request) {
        if (request == null) {
            return null;
        }
        BoardingRequestResponse response = new BoardingRequestResponse();
        response.setId(request.getId());
        response.setPassenger(passengerMapper.toResponse(request.getPassenger()));
        response.setTrip(tripMapper.toResponse(request.getTrip()));
        response.setBoardingStop(stopMapper.toResponse(request.getBoardingStop()));
        response.setDestinationStop(stopMapper.toResponse(request.getDestinationStop()));
        if (request.getPassengerRecord() != null) {
            response.setPassengerRecordId(request.getPassengerRecord().getId());
        }
        response.setStatus(request.getStatus());
        response.setNote(request.getNote());
        response.setRequestedAt(request.getRequestedAt());
        response.setBluetoothIdentifier("BR-" + request.getId());
        return response;
    }
}
