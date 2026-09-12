package com.smartbus.backend.repository;

import com.smartbus.backend.entity.AiClientContext;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiClientContextRepository extends JpaRepository<AiClientContext, Long> {

    Optional<AiClientContext> findByOwnerTypeAndOwnerIdAndClientKey(
            String ownerType,
            Long ownerId,
            String clientKey
    );
}
