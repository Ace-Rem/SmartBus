package com.smartbus.backend.repository;

import com.smartbus.backend.entity.Passenger;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    Optional<Passenger> findByUsername(String username);

    Optional<Passenger> findByUsernameOrPhoneNumber(String username, String phoneNumber);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);
}
