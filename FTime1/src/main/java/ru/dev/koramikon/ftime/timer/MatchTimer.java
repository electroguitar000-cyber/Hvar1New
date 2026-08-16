package ru.dev.koramikon.ftime.timer;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import ru.dev.koramikon.ftime.FTime;

public class MatchTimer {

    private final FTime plugin;
    private int secondsLeft;
    private int extraSeconds = 0;
    private String extraAddedBy = null;
    private long extraAddedTimestamp = 0;
    private boolean paused = false;
    private boolean penkaMode = false;  // false = TIME режим (по умолчанию), true = PENKA режим
    private BukkitRunnable task;
    private Location startLocation;

    public MatchTimer(FTime plugin, int seconds) {
        this.plugin = plugin;
        this.secondsLeft = seconds;
        this.penkaMode = false; // По умолчанию TIME режим
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (paused) return;

                // В режиме PENKA время не идёт
                if (penkaMode) return;

                if (secondsLeft > 0) {
                    secondsLeft--;
                } else if (extraSeconds > 0) {
                    extraSeconds--;
                } else {
                    end();
                    return;
                }

                updateActionBar();
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);

        updateActionBar();

        if (startLocation != null) {
            broadcastStart();
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        extraSeconds = 0;
        extraAddedBy = null;
        extraAddedTimestamp = 0;
    }

    // Команда /ftime penka - останавливает время навсегда
    public void setPenkaMode() {
        this.penkaMode = true;
        this.paused = false;
        updateActionBar();

        if (startLocation != null) {
            plugin.broadcastRadius(startLocation, plugin.getMatchRadius(),
                    plugin.getMessage("match.penka"));
        }
    }

    // Команда /ftime time - время идёт как обычно
    public void setTimeMode() {
        this.penkaMode = false;
        this.paused = false;
        updateActionBar();

        if (startLocation != null) {
            plugin.broadcastRadius(startLocation, plugin.getMatchRadius(),
                    plugin.getMessage("match.time"));
        }
    }

    public boolean isPenkaMode() {
        return penkaMode;
    }

    public void togglePause() {
        paused = !paused;
        updateActionBar();
    }

    public void addExtraTime(int seconds, String playerName) {
        this.extraSeconds += seconds;
        this.extraAddedBy = playerName;
        this.extraAddedTimestamp = System.currentTimeMillis();
        updateActionBar();
    }

    public void removeExtraTime(int seconds) {
        this.extraSeconds -= seconds;
        if (this.extraSeconds <= 0) {
            this.extraSeconds = 0;
            this.extraAddedBy = null;
            this.extraAddedTimestamp = 0;
        }
        updateActionBar();
    }

    public void setExtraTime(int seconds, String playerName) {
        this.extraSeconds = seconds;
        this.extraAddedBy = playerName;
        this.extraAddedTimestamp = System.currentTimeMillis();
        updateActionBar();
    }

    public boolean hasExtraTime() {
        return extraSeconds > 0 && extraAddedBy != null;
    }

    public int getExtraSeconds() {
        return extraSeconds;
    }

    public String getExtraAddedBy() {
        return extraAddedBy;
    }

    public long getExtraAddedTimestamp() {
        return extraAddedTimestamp;
    }

    public void setStartLocation(Location location) {
        this.startLocation = location;
    }

    private void updateActionBar() {
        String msg;
        int period = plugin.getCurrentPeriod();

        // Если режим PENKA - показываем STOP
        if (penkaMode) {
            msg = plugin.getMessage("actionbar.penka",
                    "period", String.valueOf(period));
        } else if (secondsLeft > 0) {
            int m = secondsLeft / 60;
            int s = secondsLeft % 60;
            String time = String.format("%02d:%02d", m, s);
            msg = plugin.getMessage("actionbar.normal",
                    "period", String.valueOf(period),
                    "time", time);
        } else if (extraSeconds > 0) {
            int m = extraSeconds / 60;
            int s = extraSeconds % 60;
            String time = String.format("%02d:%02d", m, s);
            msg = plugin.getMessage("actionbar.with_extra",
                    "period", String.valueOf(period),
                    "time", time);
        } else {
            msg = plugin.getMessage("actionbar.normal",
                    "period", String.valueOf(period),
                    "time", "00:00");
        }

        if (startLocation != null) {
            plugin.broadcastActionBar(startLocation, plugin.getActionbarRadius(), msg);
        }
    }

    private void broadcastStart() {
        if (startLocation == null) return;
        plugin.broadcastRadius(startLocation, plugin.getMatchRadius(),
                plugin.getMessage("broadcast.start"));
    }

    private void end() {
        stop();
        plugin.setMatchTimer(null);

        if (startLocation != null) {
            String endMsg = plugin.getMessage("broadcast.end");
            plugin.broadcastRadius(startLocation, plugin.getMatchRadius(), endMsg);
            plugin.broadcastRadius(startLocation, plugin.getMatchRadius(), endMsg);
            plugin.broadcastRadius(startLocation, plugin.getMatchRadius(), endMsg);
        }
    }
}