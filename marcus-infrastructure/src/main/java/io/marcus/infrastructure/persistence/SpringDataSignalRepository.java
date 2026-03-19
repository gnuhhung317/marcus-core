package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.SignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringDataSignalRepository extends JpaRepository<SignalEntity, String> {
    Optional<SignalEntity> findBySignalId(String signalId);
}
