package com.smartbus.backend.repository;

import com.smartbus.backend.entity.BoardingRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardingRequestRepository extends JpaRepository<BoardingRequest, Long> {

    List<BoardingRequest> findByPassengerIdOrderByRequestedAtDesc(Long passengerId);

    List<BoardingRequest> findByTripIdOrderByRequestedAtAsc(Long tripId);

    @Query("""
            SELECT br FROM BoardingRequest br
            JOIN FETCH br.passenger
            JOIN FETCH br.trip t
            JOIN FETCH t.driver
            JOIN FETCH t.route
            JOIN FETCH br.boardingStop
            JOIN FETCH br.destinationStop
            LEFT JOIN FETCH br.passengerRecord
            WHERE br.id = :id
            """)
    Optional<BoardingRequest> findByIdWithDetails(@Param("id") Long id);
}
