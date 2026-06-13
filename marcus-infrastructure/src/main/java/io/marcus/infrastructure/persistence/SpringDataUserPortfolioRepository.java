package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataUserPortfolioRepository extends JpaRepository<UserPortfolioEntity, String> {

    Optional<UserPortfolioEntity> findByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPortfolioEntity p where p.userId = :userId")
    Optional<UserPortfolioEntity> findByUserIdForUpdate(@Param("userId") String userId);

}
