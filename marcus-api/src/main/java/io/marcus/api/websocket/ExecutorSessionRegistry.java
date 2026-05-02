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
 * Registry that manages live WebSocket sessions for Executor Clients.
 *
 * Terminology:
 *  - Signal Bot (botId)     : a trading strategy that generates signals. Multiple users can subscribe.
 *  - Executor Client (wsToken): a unique running instance per user-bot subscription.
 *                              Each subscription has exactly one wsToken issued at subscribe time.
 *
 * Invariants enforced:
 *  1. Single-session-per-wsToken: only one live WebSocket session per wsToken.
 *     If a new connection arrives with the same wsToken, the old one is immediately
 *     closed with CloseStatus.POLICY_VIOLATION ("Superseded by a new connection").
 *  2. broadcastToBot(botId) fans out a signal frame to ALL active executor sessions
 *     that are subscribed to that botId (i.e. different users, different wsTokens, same bot).
 */
@Component
@Slf4j
public class ExecutorSessionRegistry {

    /** Primary store: wsToken → single active WebSocket session */
    private final ConcurrentHashMap<String, WebSocketSession> sessionByWsToken = new ConcurrentHashMap<>();

    /** Secondary index: botId → set of wsTokens (one per subscriber) */
    private final ConcurrentHashMap<String, Set<String>> wsTokensByBotId = new ConcurrentHashMap<>();

    /** Reverse lookup for disconnect cleanup: sessionId → wsToken */
    private final ConcurrentHashMap<String, String> wsTokenBySessionId = new ConcurrentHashMap<>();

    /** For unregister cleanup: wsToken → botId */
    private final ConcurrentHashMap<String, String> botIdByWsToken = new ConcurrentHashMap<>();

    /**
     * Register an executor client session.
     * If a session already exists for the given wsToken, it is terminated immediately
     * (Single Session Policy) before the new session is accepted.
     *
     * @param wsToken unique token for this user-bot subscription
     * @param botId   the signal bot this executor client is subscribing to
     * @param session the incoming WebSocket session
     */
    public void register(String wsToken, String botId, WebSocketSession session) {
        WebSocketSession existing = sessionByWsToken.put(wsToken, session);

        if (existing != null && !existing.getId().equals(session.getId())) {
            log.warn("[SessionRegistry] wsToken={} already has active session={}. Terminating it (Single Session Policy).",
                    wsToken, existing.getId());
            try {
                if (existing.isOpen()) {
                    existing.close(CloseStatus.POLICY_VIOLATION.withReason("Superseded by a new connection"));
                }
            } catch (IOException e) {
                log.warn("[SessionRegistry] Could not close old session={}: {}", existing.getId(), e.getMessage());
            }
            wsTokenBySessionId.remove(existing.getId());
        }

        wsTokenBySessionId.put(session.getId(), wsToken);
        botIdByWsToken.put(wsToken, botId);
        wsTokensByBotId.computeIfAbsent(botId, k -> ConcurrentHashMap.newKeySet()).add(wsToken);

        log.info("[SessionRegistry] Registered executor session={} wsToken={} botId={}",
                session.getId(), wsToken, botId);
    }

    /**
     * Unregister a session when its WebSocket connection is closed (normal or abnormal).
     */
    public void unregister(WebSocketSession session) {
        String wsToken = wsTokenBySessionId.remove(session.getId());
        if (wsToken == null) {
            return;
        }

        sessionByWsToken.remove(wsToken);

        String botId = botIdByWsToken.remove(wsToken);
        if (botId != null) {
            wsTokensByBotId.computeIfPresent(botId, (k, tokens) -> {
                tokens.remove(wsToken);
                return tokens.isEmpty() ? null : tokens;
            });
        }

        log.info("[SessionRegistry] Unregistered session={} wsToken={} botId={}",
                session.getId(), wsToken, botId);
    }

    /**
     * Broadcast a signal frame to ALL active executor clients subscribed to the given botId.
     * Stale (closed) sessions are cleaned up lazily during the broadcast iteration.
     *
     * @param botId the signal bot whose subscribers should receive the frame
     * @param frame JSON string to send
     */
    public void broadcastToBot(String botId, String frame) {
        Set<String> wsTokens = wsTokensByBotId.get(botId);
        if (wsTokens == null || wsTokens.isEmpty()) {
            log.debug("[SessionRegistry] No active executor sessions for botId={}", botId);
            return;
        }

        TextMessage message = new TextMessage(frame);
        for (String wsToken : wsTokens.toArray(new String[0])) {
            WebSocketSession session = sessionByWsToken.get(wsToken);

            if (session == null || !session.isOpen()) {
                wsTokens.remove(wsToken);
                sessionByWsToken.remove(wsToken);
                continue;
            }

            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("[SessionRegistry] Failed to send to wsToken={}: {}", wsToken, e.getMessage());
                wsTokens.remove(wsToken);
                sessionByWsToken.remove(wsToken);
            }
        }
    }
}
