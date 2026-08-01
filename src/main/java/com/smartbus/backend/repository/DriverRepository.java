package com.smartbus.backend.repository;

import com.smartbus.backend.entity.Driver;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByUsername(String username);

    boolean existsByUsername(String username);
}
