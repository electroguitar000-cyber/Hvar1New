package ru.koramikon.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerBlockManager {

    private final Map<UUID, Set<UUID>> blockedPlayers = new HashMap<>();

    /**
     * Блокирует игрока target для телепортации от source
     */
    public void blockPlayer(UUID source, UUID target) {
        blockedPlayers.computeIfAbsent(source, k -> new HashSet<>()).add(target);
    }

    /**
     * Разблокирует игрока target для телепортации от source
     */
    public void unblockPlayer(UUID source, UUID target) {
        Set<UUID> blocks = blockedPlayers.get(source);
        if (blocks != null) {
            blocks.remove(target);
            if (blocks.isEmpty()) {
                blockedPlayers.remove(source);
            }
        }
    }

    /**
     * Проверяет, заблокировал ли source игрока target
     */
    public boolean isBlocked(UUID source, UUID target) {
        Set<UUID> blocks = blockedPlayers.get(source);
        return blocks != null && blocks.contains(target);
    }

    /**
     * Получает список всех заблокированных игроков для source
     */
    public Set<UUID> getBlockedPlayers(UUID source) {
        return blockedPlayers.getOrDefault(source, new HashSet<>());
    }

    /**
     * Очищает все блокировки для игрока (при выходе и т.д.)
     */
    public void clearBlocks(UUID source) {
        blockedPlayers.remove(source);
    }
}