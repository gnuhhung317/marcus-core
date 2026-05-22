package io.marcus.domain.port;

import java.util.Set;

/**
 * Port for tracking executor online/offline state via Redis TTL.
 *
 * <p>Key pattern: {@code marcus:executor:online:{wsToken}} with a rolling TTL.
 * Executor must call a heartbeat every ~15 s; the server extends the key TTL to 30 s on each ping.
 * If the key expires the executor is considered offline.
 */
public interface ExecutorOnlineStatusPort {

    /**
     * Mark an executor as online (or extend its TTL).
     *
     * @param wsToken     the executor's WebSocket token (unique per subscription)
     * @param ttlSeconds  how many seconds the online marker should live (typically 30)
     */
    void markOnline(String wsToken, long ttlSeconds);

    /**
     * Immediately evict the online marker (called on clean disconnect).
     *
     * @param wsToken the executor's WebSocket token
     */
    void markOffline(String wsToken);

    /**
     * Check whether a specific executor is currently online.
     *
     * @param wsToken the executor's WebSocket token
     * @return {@code true} if the Redis key exists (TTL not expired)
     */
    boolean isOnline(String wsToken);

    /**
     * Return the set of wsTokens that are currently online for a given bot.
     *
     * <p>Implemented by iterating over the active subscriptions for the bot
     * (provided by the caller) and checking each token against Redis.
     * The caller is responsible for providing the candidate token set.
     *
     * @param wsTokens candidate wsToken set (active subscriptions for a bot)
     * @return subset of tokens that are currently online
     */
    Set<String> filterOnline(Set<String> wsTokens);
}
