package com.smartbus.backend.mapper;

import com.smartbus.backend.dto.TripResponse;
import com.smartbus.backend.entity.Trip;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {

    public TripResponse toResponse(Trip trip) {
        if (trip == null) {
            return null;
        }
        TripResponse response = new TripResponse();
        response.setId(trip.getId());
        if (trip.getDriver() != null) {
            response.setDriverId(trip.getDriver().getId());
        }
        if (trip.getRoute() != null) {
            response.setRouteId(trip.getRoute().getId());
        }
        if (trip.getCurrentStop() != null) {
            response.setCurrentStopId(trip.getCurrentStop().getId());
        }
        response.setStatus(trip.getStatus());
        response.setStartedAt(trip.getStartedAt());
        response.setEndedAt(trip.getEndedAt());
        response.setCurrentLatitude(trip.getCurrentLatitude());
        response.setCurrentLongitude(trip.getCurrentLongitude());
        return response;
    }
}
