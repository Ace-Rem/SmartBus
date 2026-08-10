package com.smartbus.backend.repository;

import com.smartbus.backend.entity.Stop;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, Long> {

    List<Stop> findByActiveTrue();

    List<Stop> findByRouteIdAndActiveTrueOrderByStopOrderAsc(Long routeId);

    Optional<Stop> findFirstByRouteIdAndActiveTrueAndStopOrderGreaterThanOrderByStopOrderDesc(
            Long routeId,
            Integer stopOrder
    );

    Optional<Stop> findByRouteIdAndStopOrder(Long routeId, Integer stopOrder);

    Optional<Stop> findFirstByRouteIdAndActiveTrueAndStopOrderGreaterThanOrderByStopOrderAsc(
            Long routeId,
            Integer stopOrder
    );

    Optional<Stop> findByIdAndRouteId(Long id, Long routeId);
}
