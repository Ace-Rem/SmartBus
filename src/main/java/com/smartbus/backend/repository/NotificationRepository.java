package com.smartbus.backend.repository;

import com.smartbus.backend.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByPassengerIdOrderByCreatedAtDesc(Long passengerId);
}
