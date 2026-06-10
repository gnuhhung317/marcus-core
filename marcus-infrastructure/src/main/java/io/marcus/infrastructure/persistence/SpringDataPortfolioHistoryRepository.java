package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.PortfolioBalanceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpringDataPortfolioHistoryRepository extends JpaRepository<PortfolioBalanceHistoryEntity, String> {

    List<PortfolioBalanceHistoryEntity> findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(String userId, LocalDateTime from);

}
