package com.smartbus.backend.mapper;

import com.smartbus.backend.dto.PassengerResponse;
import com.smartbus.backend.entity.Passenger;
import org.springframework.stereotype.Component;

@Component
public class PassengerMapper {

    public PassengerResponse toResponse(Passenger passenger) {
        if (passenger == null) {
            return null;
        }
        PassengerResponse response = new PassengerResponse();
        response.setId(passenger.getId());
        response.setFullName(passenger.getFullName());
        response.setPhoneNumber(passenger.getPhoneNumber());
        response.setUsername(passenger.getUsername());
        return response;
    }
}
