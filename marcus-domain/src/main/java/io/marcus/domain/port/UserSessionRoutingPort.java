package io.marcus.domain.port;

import java.util.Set;

public interface UserSessionRoutingPort {

    void upsertSession(String userId, String sessionId, String serverId);

    void removeSession(String userId, String sessionId);

    Set<String> findServerIdsByUserIds(Set<String> userIds);
}
