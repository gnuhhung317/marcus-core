package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.BotFavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataBotFavoriteRepository extends JpaRepository<BotFavoriteEntity, String> {

    Optional<BotFavoriteEntity> findByUserIdAndBotId(String userId, String botId);
}
