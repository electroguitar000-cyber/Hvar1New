package org.anonymous.withercps.sessions.watcher;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WatcherService {

    private final Map<UUID, WatcherSession> sessions = new ConcurrentHashMap<>();

    public WatcherSession start(UUID uuid) {
        return sessions.computeIfAbsent(uuid, id -> new WatcherSession());
    }

    public void stop(UUID uuid) {
        WatcherSession session = sessions.get(uuid);

        if (session != null) {
            session.setNetwork(false);
            session.setServer(false);
        }
    }

    public WatcherSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public Map<UUID, WatcherSession> getSessions() {
        return sessions;
    }

    public void terminate(UUID uuid) {
        sessions.remove(uuid);
    }
}