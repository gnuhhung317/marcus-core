package io.marcus.api.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExecutorSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByBotId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIdByBotId = new ConcurrentHashMap<>();

    public void register(String botId, WebSocketSession session) {
        sessionsByBotId.compute(botId, (key, sessions) -> {
            Set<WebSocketSession> targetSessions = sessions != null ? sessions : ConcurrentHashMap.newKeySet();
            targetSessions.add(session);
            return targetSessions;
        });
        sessionIdByBotId.put(session.getId(), botId);
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
