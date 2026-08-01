package com.smartbus.backend.repository;

import com.smartbus.backend.entity.Trip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByDriverIdAndStatus(Long driverId, String status);

    List<Trip> findByDriverIdAndStatusOrderByStartedAtDesc(Long driverId, String status);

    List<Trip> findByDriverIdOrderByStartedAtDesc(Long driverId);

    @Query("""
            SELECT t FROM Trip t
            JOIN FETCH t.driver
            JOIN FETCH t.route
            LEFT JOIN FETCH t.currentStop
            WHERE t.id = :id
            """)
    Optional<Trip> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT t FROM Trip t
            JOIN FETCH t.driver
            JOIN FETCH t.route
            LEFT JOIN FETCH t.currentStop
            WHERE t.driver.id = :driverId AND t.status = :status
            """)
    Optional<Trip> findCurrentTripWithDetails(
            @Param("driverId") Long driverId,
            @Param("status") String status
    );
}
