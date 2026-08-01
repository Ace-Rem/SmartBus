package com.smartbus.backend.service;

import com.smartbus.backend.dto.RouteResponse;
import com.smartbus.backend.dto.StopResponse;
import java.util.List;

public interface RouteService {

    List<RouteResponse> listActiveRoutes();

    RouteResponse getById(Long routeId);

    List<StopResponse> listStopsByRoute(Long routeId);
}
