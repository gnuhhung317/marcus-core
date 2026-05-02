package io.marcus.api.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry mapping Executor Client sessions to their bot subscriptions.
 *
 * Terminology:
 *  - Signal Bot (botId)      : a strategy generating signals; multiple users can subscribe.
 *  - Executor Client (wsToken): one unique token per user-bot subscription.
 *                               Only ONE running instance is allowed per wsToken.
 *
 * Invariants:
 *  1. Single-session-per-wsToken: new connection for an existing wsToken evicts the old one.
 *  2. broadcastToBot(botId)  : fans out to ALL active wsTokens subscribed to that bot.
 *
 * Thread-safety approach:
 *  - All map mutations in register() happen BEFORE the I/O side-effect (session.close()).
 *    This prevents afterConnectionClosed → unregister() from racing with register().
 *  - unregister() uses ConcurrentHashMap.remove(key, value) (compare-and-remove) so it
 *    never evicts a session that register() has already replaced.
 *  - Cleanup is centralised in cleanupSecondaryIndexes() and cleanupStaleToken() to
 *    prevent partial-cleanup leaks across the 4 maps.
 *
 * Distributed note (known limitation):
 *  This registry is in-memory. For multi-instance deployments use sticky sessions
 *  (Level 1) or a Redis-backed wsToken→instanceId index (Level 2).
 */
@Component
@Slf4j
public class ExecutorSessionRegistry {

    /** Primary: wsToken → active WebSocket session */
    private final ConcurrentHashMap<String, WebSocketSession> sessionByWsToken = new ConcurrentHashMap<>();

    /** Secondary index: botId → Set<wsToken> (all subscribers of a bot) */
    private final ConcurrentHashMap<String, Set<String>> wsTokensByBotId = new ConcurrentHashMap<>();

    /** Reverse lookup: sessionId → wsToken  (disconnect cleanup) */
    private final ConcurrentHashMap<String, String> wsTokenBySessionId = new ConcurrentHashMap<>();

    /** Reverse lookup: wsToken → subscriptionId (DB sync) */
    private final ConcurrentHashMap<String, String> subscriptionIdByWsToken = new ConcurrentHashMap<>();

    /** Reverse: wsToken → botId  (secondary index cleanup) */
    private final ConcurrentHashMap<String, String> botIdByWsToken = new ConcurrentHashMap<>();

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Register an executor client session.
     */
    public void register(String wsToken, String botId, String subscriptionId, WebSocketSession session) {
        // ── Step 1: claim the slot (mutations only, no I/O) ──────────────────
        WebSocketSession displaced = sessionByWsToken.put(wsToken, session);

        if (displaced != null && !displaced.getId().equals(session.getId())) {
            wsTokenBySessionId.remove(displaced.getId());
        }

        wsTokenBySessionId.put(session.getId(), wsToken);
        botIdByWsToken.put(wsToken, botId);
        subscriptionIdByWsToken.put(wsToken, subscriptionId);
        wsTokensByBotId.computeIfAbsent(botId, k -> ConcurrentHashMap.newKeySet()).add(wsToken);

        log.info("[SessionRegistry] Registered session={} wsToken={} botId={} subscriptionId={}",
                session.getId(), wsToken, botId, subscriptionId);

        // ── Step 2: I/O side-effect AFTER all mutations ───────────────────────
        if (displaced != null && !displaced.getId().equals(session.getId())) {
            log.warn("[SessionRegistry] wsToken={} evicting old session={} (Single Session Policy)",
                    wsToken, displaced.getId());
            closeQuietly(displaced, CloseStatus.POLICY_VIOLATION.withReason("Superseded by a new connection"));
        }
    }

    /**
     * Unregister a session on WebSocket close.
     * Returns the subscriptionId if the session was the active owner, null otherwise.
     */
    public String unregister(WebSocketSession session) {
        String wsToken = wsTokenBySessionId.remove(session.getId());
        if (wsToken == null) {
            return null;
        }

        String subscriptionId = subscriptionIdByWsToken.get(wsToken);
        boolean wasOwner = sessionByWsToken.remove(wsToken, session);

        if (wasOwner) {
            cleanupSecondaryIndexes(wsToken);
            subscriptionIdByWsToken.remove(wsToken);
            log.info("[SessionRegistry] Unregistered session={} wsToken={} wasOwner=true",
                    session.getId(), wsToken);
            return subscriptionId;
        }

        log.info("[SessionRegistry] Unregistered session={} wsToken={} wasOwner=false",
                session.getId(), wsToken);
        return null;
    }

    /**
     * Broadcast a signal frame to ALL active executor clients subscribed to botId.
     * Stale sessions are cleaned up lazily via compare-and-remove.
     *
     * Fix #4: cleanupStaleToken() removes from all 4 maps, not just 2.
     */
    public void broadcastToBot(String botId, String frame) {
        Set<String> wsTokens = wsTokensByBotId.get(botId);
        if (wsTokens == null || wsTokens.isEmpty()) {
            log.debug("[SessionRegistry] No active executors for botId={}", botId);
            return;
        }

        TextMessage message = new TextMessage(frame);
        for (String wsToken : wsTokens.toArray(new String[0])) {
            WebSocketSession session = sessionByWsToken.get(wsToken);

            if (session == null || !session.isOpen()) {
                log.debug("[SessionRegistry] Cleaning up stale wsToken={} for botId={}", wsToken, botId);
                cleanupStaleToken(wsToken, session);
                continue;
            }

            try {
                // Note: This is blocking I/O. For high-scale, consider using a per-session queue.
                session.sendMessage(message);
            } catch (IOException | IllegalStateException e) {
                log.warn("[SessionRegistry] Failed to send to wsToken={} (session={}): {}", 
                        wsToken, session.getId(), e.getMessage());
                // Immediate cleanup on failure to prevent pulling down the loop
                cleanupStaleToken(wsToken, session);
            }
        }
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    /**
     * Clean up secondary indexes for a wsToken we already confirmed ownership of.
     * Fix #4: always removes from botIdByWsToken AND wsTokensByBotId.
     */
    private void cleanupSecondaryIndexes(String wsToken) {
        String botId = botIdByWsToken.remove(wsToken);
        if (botId != null) {
            wsTokensByBotId.computeIfPresent(botId, (k, tokens) -> {
                tokens.remove(wsToken);
                return tokens.isEmpty() ? null : tokens;
            });
        }
    }

    /**
     * Lazily clean up a stale wsToken discovered during broadcast.
     * Uses compare-and-remove so a legitimately replaced (live) session is never evicted.
     */
    private void cleanupStaleToken(String wsToken, WebSocketSession staleSession) {
        if (staleSession != null) {
            if (sessionByWsToken.remove(wsToken, staleSession)) {
                wsTokenBySessionId.remove(staleSession.getId());
                subscriptionIdByWsToken.remove(wsToken);
                cleanupSecondaryIndexes(wsToken);
            } else {
                // Convergence fix: primary already points elsewhere or null, 
                // but we should still ensure secondary indexes are consistent if primary is null
                if (sessionByWsToken.get(wsToken) == null) {
                    cleanupSecondaryIndexes(wsToken);
                    subscriptionIdByWsToken.remove(wsToken);
                }
            }
        } else {
            cleanupSecondaryIndexes(wsToken);
            subscriptionIdByWsToken.remove(wsToken);
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException e) {
            log.warn("[SessionRegistry] Could not close session={}: {}", session.getId(), e.getMessage());
        }
    }
}
