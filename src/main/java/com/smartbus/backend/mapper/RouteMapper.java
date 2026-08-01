package com.smartbus.backend.mapper;

import com.smartbus.backend.dto.RouteResponse;
import com.smartbus.backend.entity.Route;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper {

    public RouteResponse toResponse(Route route) {
        if (route == null) {
            return null;
        }
        RouteResponse response = new RouteResponse();
        response.setId(route.getId());
        response.setCode(route.getCode());
        response.setName(route.getName());
        response.setDescription(route.getDescription());
        return response;
    }
}
