package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.PortfolioAggregateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpringDataPortfolioAggregateHistoryRepository extends JpaRepository<PortfolioAggregateHistoryEntity, String> {

    List<PortfolioAggregateHistoryEntity> findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(String userId, LocalDateTime from);
}
