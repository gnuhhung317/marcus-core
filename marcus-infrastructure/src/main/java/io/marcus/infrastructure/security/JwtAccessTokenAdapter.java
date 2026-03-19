package io.marcus.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.marcus.domain.port.AccessTokenPort;
import io.marcus.domain.port.RefreshTokenPort;
import io.marcus.domain.vo.AuthenticatedUser;
import io.marcus.domain.vo.Role;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAccessTokenAdapter implements AccessTokenPort, RefreshTokenPort {

    private static final String ROLE_CLAIM = "role";
    private static final String USERNAME_CLAIM = "username";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String JTI_CLAIM = "jti";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";
    private static final String REFRESH_ACTIVE_PREFIX = "auth:refresh:active:";
    private static final String REFRESH_USED_PREFIX = "auth:refresh:used:";

    private final StringRedisTemplate redisTemplate;

    public JwtAccessTokenAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Value("${marcus.security.jwt.secret}")
    private String jwtSecret;

    @Value("${marcus.security.jwt.access-token-expiration-seconds:3600}")
    private long accessTokenExpirationSeconds;

    @Value("${marcus.security.jwt.refresh-token-expiration-seconds:604800}")
    private long refreshTokenExpirationSeconds;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    @Override
    public String issueToken(AuthenticatedUser authenticatedUser) {
        return buildToken(authenticatedUser, ACCESS_TOKEN_TYPE, accessTokenExpirationSeconds);
    }

    @Override
    public String issueRefreshToken(AuthenticatedUser authenticatedUser) {
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = buildToken(authenticatedUser, REFRESH_TOKEN_TYPE, refreshTokenExpirationSeconds, tokenId);

        String activeRefreshKey = activeRefreshKey(authenticatedUser.userId());
        redisTemplate.opsForValue().set(activeRefreshKey, tokenId, Duration.ofSeconds(refreshTokenExpirationSeconds));

        return refreshToken;
    }

    private String buildToken(AuthenticatedUser authenticatedUser, String tokenType, long ttlSeconds) {
        return buildToken(authenticatedUser, tokenType, ttlSeconds, null);
    }

    private String buildToken(AuthenticatedUser authenticatedUser, String tokenType, long ttlSeconds, String tokenId) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
                .subject(authenticatedUser.userId())
                .claim(USERNAME_CLAIM, authenticatedUser.username())
                .claim(ROLE_CLAIM, authenticatedUser.role().name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)));

        if (tokenId != null) {
            builder.id(tokenId).claim(JTI_CLAIM, tokenId);
        }

        return builder.signWith(signingKey).compact();
    }

    @Override
    public Optional<AuthenticatedUser> parseToken(String token) {
        return parseByTokenType(token, ACCESS_TOKEN_TYPE);
    }

    @Override
    public Optional<AuthenticatedUser> consumeRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
                return Optional.empty();
            }

            String userId = claims.getSubject();
            String username = claims.get(USERNAME_CLAIM, String.class);
            String roleValue = claims.get(ROLE_CLAIM, String.class);
            String tokenId = claims.get(JTI_CLAIM, String.class);

            if (userId == null || username == null || roleValue == null || tokenId == null) {
                return Optional.empty();
            }

            String usedRefreshKey = usedRefreshKey(tokenId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(usedRefreshKey))) {
                // Reuse detected for an already consumed token: revoke current active token for this user.
                redisTemplate.delete(activeRefreshKey(userId));
                return Optional.empty();
            }

            String activeTokenId = redisTemplate.opsForValue().get(activeRefreshKey(userId));
            if (!tokenId.equals(activeTokenId)) {
                return Optional.empty();
            }

            long usedTtl = secondsUntilExpiry(claims.getExpiration());
            if (usedTtl > 0) {
                redisTemplate.opsForValue().set(usedRefreshKey, userId, Duration.ofSeconds(usedTtl));
            }

            Role role = Role.valueOf(roleValue);
            return Optional.of(new AuthenticatedUser(userId, username, role));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Optional<AuthenticatedUser> parseByTokenType(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!expectedType.equals(tokenType)) {
                return Optional.empty();
            }

            String userId = claims.getSubject();
            String username = claims.get(USERNAME_CLAIM, String.class);
            String roleValue = claims.get(ROLE_CLAIM, String.class);

            if (userId == null || username == null || roleValue == null) {
                return Optional.empty();
            }

            Role role = Role.valueOf(roleValue);
            return Optional.of(new AuthenticatedUser(userId, username, role));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public long accessTokenTtlSeconds() {
        return accessTokenExpirationSeconds;
    }

    @Override
    public long refreshTokenTtlSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private String activeRefreshKey(String userId) {
        return REFRESH_ACTIVE_PREFIX + userId;
    }

    private String usedRefreshKey(String tokenId) {
        return REFRESH_USED_PREFIX + tokenId;
    }

    private long secondsUntilExpiry(Date expiresAt) {
        long seconds = (expiresAt.toInstant().toEpochMilli() - System.currentTimeMillis()) / 1000;
        return Math.max(seconds, 0);
    }
}
