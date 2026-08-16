package ru.koramikon.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerSettingsManager {

    private final Set<UUID> autoDeny = new HashSet<>();
    private final Set<UUID> tpNoticeEnabled = new HashSet<>();      // для /tp
    private final Set<UUID> tpaNoticeEnabled = new HashSet<>();     // для /tpa
    private final Set<UUID> tpDisabled = new HashSet<>();

    public boolean isAutoDeny(UUID playerUuid) {
        return autoDeny.contains(playerUuid);
    }

    public void setAutoDeny(UUID playerUuid, boolean enabled) {
        if (enabled) {
            autoDeny.add(playerUuid);
        } else {
            autoDeny.remove(playerUuid);
        }
    }

    public boolean isTpNoticeEnabled(UUID playerUuid) {
        return tpNoticeEnabled.contains(playerUuid);
    }

    public void setTpNoticeEnabled(UUID playerUuid, boolean enabled) {
        if (enabled) {
            tpNoticeEnabled.add(playerUuid);
        } else {
            tpNoticeEnabled.remove(playerUuid);
        }
    }

    public boolean isTpaNoticeEnabled(UUID playerUuid) {
        return tpaNoticeEnabled.contains(playerUuid);
    }

    public void setTpaNoticeEnabled(UUID playerUuid, boolean enabled) {
        if (enabled) {
            tpaNoticeEnabled.add(playerUuid);
        } else {
            tpaNoticeEnabled.remove(playerUuid);
        }
    }

    public boolean canTpTo(UUID playerUuid) {
        return !tpDisabled.contains(playerUuid);
    }

    public void setTpDisabled(UUID playerUuid, boolean disabled) {
        if (disabled) {
            tpDisabled.add(playerUuid);
        } else {
            tpDisabled.remove(playerUuid);
        }
    }
}