package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.ExchangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataExchangeRepository extends JpaRepository<ExchangeEntity, String> {
    Optional<ExchangeEntity> findByExchangeId(String exchangeId);
}