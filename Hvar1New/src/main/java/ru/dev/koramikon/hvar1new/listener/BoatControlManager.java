package ru.dev.koramikon.hvar1new.listener;

import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoatControlManager {

    private final Hvar1NewPlugin plugin;
    private final Map<UUID, BoatControlSession> sessions = new ConcurrentHashMap<>();

    public BoatControlManager(Hvar1NewPlugin plugin) {
        this.plugin = plugin;
        startTracker();
    }

    public void registerHit(Player player, Boat boat, int arenaId) {
        if (!plugin.gatesEnabledArr[arenaId]) return;

        UUID boatId = boat.getUniqueId();
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        BoatControlSession session = sessions.get(boatId);
        if (session == null) {
            // Первый удар – создаём сессию
            sessions.put(boatId, new BoatControlSession(boatId, arenaId, playerId, now));
            return;
        }
        if (session.currentOwner.equals(playerId)) {
            // Тот же игрок – просто обновляем время последнего удара (не влияет на таймер)
            session.lastActivity = now;
            return;
        }
        // Смена владельца – начисляем время предыдущему
        long elapsed = now - session.lastTickTime;
        if (elapsed > 0) {
            plugin.statsManagers[arenaId].addPossessionTime(session.currentOwner, elapsed);
        }
        // Начинаем новую сессию для нового владельца
        session.currentOwner = playerId;
        session.lastTickTime = now;
        session.lastActivity = now;
    }

    public void onBoatRemove(UUID boatId, int arenaId) {
        if (!plugin.gatesEnabledArr[arenaId]) return;

        BoatControlSession session = sessions.remove(boatId);
        if (session != null) {
            long elapsed = System.currentTimeMillis() - session.lastTickTime;
            if (elapsed > 0) {
                plugin.statsManagers[arenaId].addPossessionTime(session.currentOwner, elapsed);
            }
        }
    }

    private void startTracker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (BoatControlSession session : sessions.values()) {
                    // Проверяем, существует ли лодка
                    Boat boat = (Boat) plugin.getServer().getEntity(session.boatId);
                    if (boat == null || !boat.isValid()) {
                        long elapsed = now - session.lastTickTime;
                        if (elapsed > 0 && plugin.gatesEnabledArr[session.arenaId]) {
                            plugin.statsManagers[session.arenaId].addPossessionTime(session.currentOwner, elapsed);
                        }
                        sessions.remove(session.boatId);
                        continue;
                    }

                    // Начисляем время за прошедший интервал (каждую секунду)
                    long elapsed = now - session.lastTickTime;
                    if (elapsed > 0 && plugin.gatesEnabledArr[session.arenaId]) {
                        plugin.statsManagers[session.arenaId].addPossessionTime(session.currentOwner, elapsed);
                        session.lastTickTime = now;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 20 тиков = 1 секунда
    }

    private static class BoatControlSession {
        final UUID boatId;
        final int arenaId;
        UUID currentOwner;
        long lastTickTime;   // время последнего начисления
        long lastActivity;   // время последнего удара (для информации)

        BoatControlSession(UUID boatId, int arenaId, UUID owner, long startTime) {
            this.boatId = boatId;
            this.arenaId = arenaId;
            this.currentOwner = owner;
            this.lastTickTime = startTime;
            this.lastActivity = startTime;
        }
    }
}