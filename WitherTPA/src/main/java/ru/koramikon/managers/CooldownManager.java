package ru.koramikon.managers;

import org.bukkit.entity.Player;
import ru.koramikon.WitherTPA;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final WitherTPA plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownManager(WitherTPA plugin) {
        this.plugin = plugin;
    }

    public void setCooldown(UUID playerUuid) {
        int cooldownTime = plugin.getConfig().getInt("settings.teleport-cooldown", 10);
        cooldowns.put(playerUuid, System.currentTimeMillis() + (cooldownTime * 1000L));
    }

    public boolean hasCooldown(UUID playerUuid) {
        if (!cooldowns.containsKey(playerUuid)) {
            return false;
        }

        long cooldownEnd = cooldowns.get(playerUuid);
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(playerUuid);
            return false;
        }

        return true;
    }

    public int getRemainingCooldown(UUID playerUuid) {
        if (!cooldowns.containsKey(playerUuid)) {
            return 0;
        }

        long remaining = (cooldowns.get(playerUuid) - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }

    public void removeCooldown(UUID playerUuid) {
        cooldowns.remove(playerUuid);
    }
}