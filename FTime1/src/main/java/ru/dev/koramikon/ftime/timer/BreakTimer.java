package ru.dev.koramikon.ftime.timer;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import ru.dev.koramikon.ftime.FTime;

public class BreakTimer {

    private final FTime plugin;
    private final int totalSeconds;
    private int secondsLeft;
    private BukkitRunnable task;
    private Location startLocation;

    public BreakTimer(FTime plugin, int seconds) {
        this.plugin = plugin;
        this.totalSeconds = seconds;
        this.secondsLeft = seconds;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (secondsLeft > 0) {
                    secondsLeft--;
                    updateActionBar();
                } else {
                    end();
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);

        if (startLocation != null) {
            broadcastStart();
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void setStartLocation(Location location) {
        this.startLocation = location;
    }

    private void updateActionBar() {
        if (startLocation == null) return;

        int m = secondsLeft / 60;
        int s = secondsLeft % 60;
        String time = String.format("%02d:%02d", m, s);
        String msg = plugin.getMessage("actionbar.break", "time", time);

        plugin.broadcastActionBar(startLocation, plugin.getActionbarRadius(), msg);
    }

    private void broadcastStart() {
        if (startLocation == null) return;
        plugin.broadcastRadius(startLocation, plugin.getMatchRadius(),
                plugin.getMessage("break.started", "seconds", String.valueOf(totalSeconds)));
    }

    private void end() {
        stop();
        plugin.setBreakTimer(null);
        plugin.setBreakMode(false);

        if (startLocation != null) {
            plugin.broadcastRadius(startLocation, plugin.getMatchRadius(),
                    plugin.getMessage("break.ended"));
        }
    }
}