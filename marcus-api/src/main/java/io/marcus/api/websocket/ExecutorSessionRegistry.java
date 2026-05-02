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
 * Registry that enforces a single-session-per-bot policy.
 * When a new session registers for a given botId, all existing sessions
 * are terminated with POLICY_VIOLATION to prevent duplicate signal execution.
 */
@Component
@Slf4j
public class ExecutorSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByBotId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIdByBotId = new ConcurrentHashMap<>();

    /**
     * Register a new WebSocket session for the given botId.
     * Any previously active sessions for this botId will be terminated immediately.
     */
    public void register(String botId, WebSocketSession session) {
        sessionsByBotId.compute(botId, (key, existingSessions) -> {
            if (existingSessions != null && !existingSessions.isEmpty()) {
                log.warn("[SessionRegistry] botId={} has {} active session(s). Terminating old sessions (Single Session Policy).",
                        botId, existingSessions.size());
                existingSessions.forEach(old -> {
                    try {
                        old.close(CloseStatus.POLICY_VIOLATION.withReason("Superseded by a new connection"));
                        log.info("[SessionRegistry] Closed old session={} for botId={}", old.getId(), botId);
                    } catch (IOException e) {
                        log.warn("[SessionRegistry] Failed to close old session={} for botId={}: {}", old.getId(), botId, e.getMessage());
                    }
                    sessionIdByBotId.remove(old.getId());
                });
            }
            Set<WebSocketSession> newSessions = ConcurrentHashMap.newKeySet();
            newSessions.add(session);
            return newSessions;
        });
        sessionIdByBotId.put(session.getId(), botId);
        log.info("[SessionRegistry] Registered new session={} for botId={}", session.getId(), botId);
    }

    public void unregister(WebSocketSession session) {
        String botId = sessionIdByBotId.remove(session.getId());
        if (botId == null) {
            return;
        }

        sessionsByBotId.computeIfPresent(botId, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void broadcastToBot(String botId, String frame) {
        Set<WebSocketSession> sessions = sessionsByBotId.get(botId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage message = new TextMessage(frame);
        for (WebSocketSession session : sessions.toArray(new WebSocketSession[0])) {
            if (!session.isOpen()) {
                unregister(session);
                continue;
            }

            try {
                session.sendMessage(message);
            } catch (IOException exception) {
                unregister(session);
            }
        }
    }
}
