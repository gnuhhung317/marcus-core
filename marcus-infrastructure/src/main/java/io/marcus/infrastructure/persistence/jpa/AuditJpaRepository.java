package io.marcus.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditJpaRepository extends JpaRepository<JpaAuditRecordEntity, String> {
}
