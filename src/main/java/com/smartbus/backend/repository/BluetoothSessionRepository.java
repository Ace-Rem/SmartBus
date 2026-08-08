package com.smartbus.backend.repository;

import com.smartbus.backend.entity.BluetoothSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BluetoothSessionRepository extends JpaRepository<BluetoothSession, Long> {

    Optional<BluetoothSession> findFirstByIdentifierAndActiveTrueOrderByCreatedAtDesc(String identifier);
}
