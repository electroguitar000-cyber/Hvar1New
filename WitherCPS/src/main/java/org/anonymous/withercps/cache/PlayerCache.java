package org.anonymous.withercps.cache;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache {

    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<String, UUID> uuids = new ConcurrentHashMap<>();

    public void add(UUID uuid, String nickname) {
        String normalized = nickname.toLowerCase();

        if (uuids.containsKey(normalized)) {
            return;
        }

        names.put(uuid, nickname);
        uuids.put(normalized, uuid);
    }

    public UUID getUuid(String nickname) {
        return uuids.get(nickname.toLowerCase());
    }

    public String getName(UUID uuid) {
        return names.get(uuid);
    }
}