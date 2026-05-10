package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataUserPortfolioRepository extends JpaRepository<UserPortfolioEntity, String> {

    Optional<UserPortfolioEntity> findByUserId(String userId);

}
