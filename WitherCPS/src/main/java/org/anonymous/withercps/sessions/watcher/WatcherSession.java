package org.anonymous.withercps.sessions.watcher;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WatcherSession {

    private boolean network_thread;
    private boolean server_thread;

    private String mode = null;

    private final Set<UUID> targets = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> muted = new ConcurrentHashMap<>();


    public void setNetwork(boolean enable) {
        this.network_thread = enable;
    }

    public void setServer(boolean enable) {
        this.server_thread = enable;
    }

    public boolean isEnabled() {
        return network_thread || server_thread;
    }

    public boolean isNetwork() {
        return network_thread;
    }

    public boolean isServer() {
        return server_thread;
    }


    public void setMode(String mode) {
        this.mode = mode == null ? "default" : mode;
    }

    public String getMode() {
        return mode;
    }


    public void addTarget(UUID uuid) {
        targets.add(uuid);
    }

    public void removeTarget(UUID uuid) {
        targets.remove(uuid);
    }

    public Set<UUID> getTargets() {
        return targets;
    }


    public void mute(UUID target, long duration) {
        muted.put(target, duration > 0 ? System.currentTimeMillis() + duration : -1L);
    }

    public void unmute(UUID target) {
        muted.remove(target);
    }

    public boolean isMuted(UUID target) {
        Long expiry = muted.get(target);
        if (expiry == null) return false;

        if (expiry > -1L && System.currentTimeMillis() > expiry) {
            muted.remove(target);
            return false;
        }

        return true;
    }

    public Map<UUID, Long> getMuted() {
        muted.entrySet().removeIf(entry -> !isMuted(entry.getKey()));
        return muted;
    }
}