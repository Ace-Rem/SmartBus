package com.smartbus.backend.mapper;

import com.smartbus.backend.dto.PassengerRecordResponse;
import com.smartbus.backend.entity.PassengerRecord;
import org.springframework.stereotype.Component;

@Component
public class PassengerRecordMapper {

    public PassengerRecordResponse toResponse(PassengerRecord passengerRecord) {
        if (passengerRecord == null) {
            return null;
        }
        PassengerRecordResponse response = new PassengerRecordResponse();
        response.setId(passengerRecord.getId());
        if (passengerRecord.getTrip() != null) {
            response.setTripId(passengerRecord.getTrip().getId());
        }
        if (passengerRecord.getStop() != null) {
            response.setStopId(passengerRecord.getStop().getId());
            response.setDestinationStopId(passengerRecord.getStop().getId());
        }
        if (passengerRecord.getBoardingStop() != null) {
            response.setBoardingStopId(passengerRecord.getBoardingStop().getId());
        }
        response.setPassengerCount(passengerRecord.getPassengerCount());
        response.setRecordedAt(passengerRecord.getRecordedAt());
        response.setNote(passengerRecord.getNote());
        response.setSource(passengerRecord.getSource());
        response.setIdempotencyKey(passengerRecord.getIdempotencyKey());
        return response;
    }
}
