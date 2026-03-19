package io.marcus.infrastructure.security;

import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotSecretProvider {

    private final BotRepository botRepository;
    private final StringRedisTemplate redisTemplate;
    private static final String BOT_SECRET_PREFIX = "bot:secret:";

    public String getEncryptedSecret(String apiKey){

        String redisKey = BOT_SECRET_PREFIX + apiKey;

        //prevent redis application crash
        try {
            String cachedSecret = redisTemplate.opsForValue().get(redisKey);
            if (cachedSecret != null) {
                return cachedSecret;
            }
        } catch (Exception e) {
            log.error("Redis connection failed, falling back to database for bot: {}", apiKey);
        }

        // fetch from db
        String encryptedSecret = botRepository.findSecretByApiKey(apiKey).orElseThrow(() -> new BotNotFoundException(apiKey));

        // update redis
        try {
            redisTemplate.opsForValue().set(redisKey, encryptedSecret, Duration.ofDays(1));
        } catch (Exception e) {
            log.warn("Failed to update secret cache in Redis: {}", e.getMessage());
        }

        return encryptedSecret;
    }
}
