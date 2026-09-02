package com.smartbus.backend.service;

import com.smartbus.backend.dto.RouteResponse;
import com.smartbus.backend.dto.RoutePathResponse;
import com.smartbus.backend.dto.StopResponse;
import java.math.BigDecimal;
import java.util.List;

public interface RouteService {

    List<RouteResponse> listActiveRoutes();

    RouteResponse getById(Long routeId);

    List<StopResponse> listStopsByRoute(Long routeId);

    RoutePathResponse routePath(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude
    );
}
