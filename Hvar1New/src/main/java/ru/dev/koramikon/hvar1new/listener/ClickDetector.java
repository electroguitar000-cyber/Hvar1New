package ru.dev.koramikon.hvar1new.listener;

import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;
import ru.dev.koramikon.hvar1new.util.MessageUtil;

import java.util.*;

public class ClickDetector implements Listener {

    private final Hvar1NewPlugin plugin;
    private final Map<UUID, ClickSession> sessions = new HashMap<>();

    public ClickDetector(Hvar1NewPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBoatHit(VehicleDamageEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;
        if (!(event.getAttacker() instanceof Player player)) return;

        // Определяем арену
        int arenaId = findArenaByProximity(boat.getLocation());
        if (arenaId == -1) {
            arenaId = findArenaByCenter(boat.getLocation());
        }
        if (arenaId == -1) {
            arenaId = findArenaByPlayer(player);
        }

        if (arenaId != -1) {
            // Только при включённых воротах
            if (plugin.gatesEnabledArr[arenaId]) {
                // Регистрация для голов/ассистов
                plugin.registerHit(arenaId, player);
                // Добавляем клик в статистику
                plugin.statsManagers[arenaId].addClick(player.getUniqueId());
                // Время владения
                plugin.getBoatControlManager().registerHit(player, boat, arenaId);
            }
            // Створы больше не проверяются здесь (перенесено в GoalListener)
        }

        // ---------- CPS (работает всегда) ----------
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        ClickSession session = sessions.get(uuid);

        if (session == null) {
            session = new ClickSession(now);
            sessions.put(uuid, session);
            scheduleEnd(player, session);
        } else {
            session.clicks++;
            session.lastClickTime = now;
            session.task.cancel();
            scheduleEnd(player, session);
        }
    }

    // Остальные методы (поиск арены, CPS) остаются без изменений
    private int findArenaByProximity(Location loc) {
        int closestArena = -1;
        double minDist = 20.0;
        for (int i = 1; i <= 50; i++) {
            Location l1 = plugin.leftGates[i].getPos1();
            if (l1 != null && l1.getWorld().equals(loc.getWorld())) {
                double d = l1.distance(loc);
                if (d < minDist) { minDist = d; closestArena = i; }
            }
            Location l2 = plugin.leftGates[i].getPos2();
            if (l2 != null && l2.getWorld().equals(loc.getWorld())) {
                double d = l2.distance(loc);
                if (d < minDist) { minDist = d; closestArena = i; }
            }
            Location r1 = plugin.rightGates[i].getPos1();
            if (r1 != null && r1.getWorld().equals(loc.getWorld())) {
                double d = r1.distance(loc);
                if (d < minDist) { minDist = d; closestArena = i; }
            }
            Location r2 = plugin.rightGates[i].getPos2();
            if (r2 != null && r2.getWorld().equals(loc.getWorld())) {
                double d = r2.distance(loc);
                if (d < minDist) { minDist = d; closestArena = i; }
            }
        }
        return closestArena;
    }

    private int findArenaByCenter(Location loc) {
        int closestArena = -1;
        double minDist = 200.0;
        for (int i = 1; i <= 50; i++) {
            Location center = plugin.centerLocations[i];
            if (center == null) continue;
            if (!center.getWorld().equals(loc.getWorld())) continue;
            double dist = center.distance(loc);
            if (dist < minDist) {
                minDist = dist;
                closestArena = i;
            }
        }
        return closestArena;
    }

    private int findArenaByPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        for (int i = 1; i <= 50; i++) {
            if (plugin.statsManagers[i].getTeamSide(playerId) != null) {
                return i;
            }
        }
        return -1;
    }

    private void scheduleEnd(Player player, ClickSession session) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                endSession(player, session);
            }
        };
        task.runTaskLater(plugin, 2L);
        session.task = task;
    }

    private void endSession(Player player, ClickSession session) {
        sessions.remove(player.getUniqueId());

        long durationMillis = session.lastClickTime - session.firstClickTime;
        double durationSec = durationMillis / 1000.0;
        if (durationSec < 0.1) durationSec = 0.1;
        double cps = session.clicks / durationSec;

        String teamName = "-";
        for (int i = 1; i <= 50; i++) {
            String side = plugin.statsManagers[i].getTeamSide(player.getUniqueId());
            if (side != null) {
                teamName = plugin.statsManagers[i].getTeamName(side);
                break;
            }
        }

        String msg = MessageUtil.get("cps.message")
                .replace("{player}", player.getName())
                .replace("{team}", teamName)
                .replace("{clicks}", String.valueOf(session.clicks))
                .replace("{time}", String.format("%.2f", durationSec))
                .replace("{cps}", String.format("%.2f", cps));
        var component = MessageUtil.fromColored(msg);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getClickCommand().isViewer(online)) {
                online.sendMessage(component);
            }
        }
    }

    private static class ClickSession {
        long firstClickTime;
        long lastClickTime;
        int clicks = 1;
        BukkitRunnable task;

        ClickSession(long time) {
            this.firstClickTime = time;
            this.lastClickTime = time;
        }
    }
}