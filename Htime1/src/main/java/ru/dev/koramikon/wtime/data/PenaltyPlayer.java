package ru.dev.koramikon.wtime.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.dev.koramikon.wtime.WTime;
import ru.dev.koramikon.wtime.WTime.TimerInstance;

import java.util.UUID;

public class PenaltyPlayer {
    public final UUID uuid;
    public final String reason;
    public int remainingSeconds;
    private final WTime plugin;
    private final int arenaId;

    public PenaltyPlayer(UUID uuid, String reason, int minutes, WTime plugin, int arenaId) {
        this.uuid = uuid;
        this.reason = reason;
        this.remainingSeconds = minutes * 60;
        this.plugin = plugin;
        this.arenaId = arenaId;
    }

    public void start() {
        TimerInstance t = plugin.timers[arenaId];
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && t.pos2 != null) {
            Location tp = t.pos2.clone().add(0.5, 1, 0.5);
            Bukkit.getScheduler().runTask(plugin, () -> p.teleport(tp));
        }

        new Thread(() -> {
            while (remainingSeconds > 0) {
                try {
                    Thread.sleep(1000);
                    TimerInstance timer = plugin.timers[arenaId];
                    if (timer.matchTimer != null && timer.matchTimer.isRunning() && timer.timersRunning) {
                        remainingSeconds--;
                    }
                } catch (InterruptedException ignored) {}
            }

            if (remainingSeconds <= 0) {
                Bukkit.getScheduler().runTask(plugin, this::release);
            }
        }).start();
    }

    public void release() {
        TimerInstance t = plugin.timers[arenaId];
        t.penalties.remove(uuid);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && t.pos1 != null) {
            p.teleport(t.pos1);
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName() != null ? offline.getName() : "Неизвестный";
        plugin.broadcastRadius(arenaId, plugin.getMessage("penalty.expired", arenaId,
                "{player}", name));
    }

    public boolean isActive() {
        return remainingSeconds > 0;
    }

    public String getFormattedRemaining() {
        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public String getName() {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : "Неизвестный";
    }
}