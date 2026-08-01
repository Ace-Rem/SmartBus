package com.smartbus.backend.repository;

import com.smartbus.backend.entity.PassengerRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassengerRecordRepository extends JpaRepository<PassengerRecord, Long> {

    List<PassengerRecord> findByTripIdOrderByRecordedAtAsc(Long tripId);

    @Query("""
            SELECT COALESCE(SUM(p.passengerCount), 0)
            FROM PassengerRecord p
            WHERE p.trip.id = :tripId AND p.stop.id = :stopId
            """)
    int sumPassengerCountByTripIdAndStopId(
            @Param("tripId") Long tripId,
            @Param("stopId") Long stopId
    );
}
