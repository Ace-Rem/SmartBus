package com.smartbus.backend.repository;

import com.smartbus.backend.entity.Route;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByActiveTrueOrderByCodeAsc();
}
