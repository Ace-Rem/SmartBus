package com.smartbus.backend.mapper;

import com.smartbus.backend.dto.StopResponse;
import com.smartbus.backend.entity.Stop;
import org.springframework.stereotype.Component;

@Component
public class StopMapper {

    public StopResponse toResponse(Stop stop) {
        if (stop == null) {
            return null;
        }
        StopResponse response = new StopResponse();
        response.setId(stop.getId());
        if (stop.getRoute() != null) {
            response.setRouteId(stop.getRoute().getId());
        }
        response.setName(stop.getName());
        response.setLatitude(stop.getLatitude());
        response.setLongitude(stop.getLongitude());
        response.setStopOrder(stop.getStopOrder());
        return response;
    }
}
