package ru.dev.koramikon.wtime.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.dev.koramikon.wtime.WTime;

import java.util.UUID;

public class PenaltyPlayer {
    public final UUID uuid;
    public final String reason;
    private final int totalSeconds;
    public int remainingSeconds;
    private final WTime plugin;

    public PenaltyPlayer(UUID uuid, String reason, int minutes, WTime plugin) {
        this.uuid = uuid;
        this.reason = reason;
        this.totalSeconds = minutes * 60;
        this.remainingSeconds = totalSeconds;
        this.plugin = plugin;
    }

    public void start() {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && plugin.pos2 != null) {
            Location tp = plugin.pos2.clone().add(0.5, 1, 0.5);
            Bukkit.getScheduler().runTask(plugin, () -> p.teleport(tp));
        }

        new Thread(() -> {
            while (remainingSeconds > 0) {
                try {
                    Thread.sleep(1000);
                    // Штраф идет ТОЛЬКО если есть активный матч И таймеры работают
                    if (plugin.matchTimer != null && plugin.matchTimer.isRunning() && plugin.timersRunning) {
                        remainingSeconds--;
                    }
                    // Если матча нет или он закончился - штраф не идет
                } catch (InterruptedException ignored) {}
            }

            if (remainingSeconds <= 0) {
                Bukkit.getScheduler().runTask(plugin, this::release);
            }
        }).start();
    }

    public void release() {
        plugin.penalties.remove(uuid);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && plugin.pos1 != null) {
            p.teleport(plugin.pos1);
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline.getName() != null ? offline.getName() : "Неизвестный";

        // Используем сообщение из конфига без [Hockey Match]
        plugin.broadcastRadius(plugin.getMessage("penalty.expired", "{player}", name));
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