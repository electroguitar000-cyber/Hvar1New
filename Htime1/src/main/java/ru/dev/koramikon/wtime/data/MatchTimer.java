package ru.dev.koramikon.wtime.data;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.scheduler.BukkitRunnable;
import ru.dev.koramikon.wtime.WTime;
import ru.dev.koramikon.wtime.WTime.TimerInstance;

public class MatchTimer {
    private int remainingSeconds;
    private int totalSeconds;
    private boolean running = false;
    private boolean isBreak;
    private boolean ended = false;
    private boolean firstTick = true;
    private boolean isRed = false;
    private int arenaId;

    public MatchTimer(int seconds, boolean isBreak, int arenaId) {
        this.remainingSeconds = seconds;
        this.totalSeconds = seconds;
        this.isBreak = isBreak;
        this.arenaId = arenaId;
    }

    public void start() {
        running = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!running || ended) {
                    cancel();
                    return;
                }

                if (firstTick) {
                    firstTick = false;
                    return;
                }

                if (remainingSeconds > 0) {
                    if (isBreak) {
                        remainingSeconds--;
                        updateBossBar();
                    } else {
                        TimerInstance t = WTime.get().timers[arenaId];
                        if (t.timersRunning) {
                            remainingSeconds--;
                        }
                    }
                }

                if (remainingSeconds == 0) {
                    WTime plugin = WTime.get();
                    TimerInstance t = plugin.timers[arenaId];

                    if (isBreak) {
                        plugin.broadcastRadius(arenaId, plugin.getMessage("break.ended", arenaId));
                        plugin.broadcastRadius(arenaId, plugin.getMessage("break.ended", arenaId));
                        plugin.broadcastRadius(arenaId, plugin.getMessage("break.ended", arenaId));

                        if (t.breakBossBar != null) {
                            t.breakBossBar.removeAll();
                            t.breakBossBar = null;
                        }

                        t.breakTimer = null;

                    } else {
                        plugin.broadcastRadius(arenaId, plugin.getMessage("match.natural-end", arenaId));
                        plugin.broadcastRadius(arenaId, plugin.getMessage("match.natural-end", arenaId));
                        plugin.broadcastRadius(arenaId, plugin.getMessage("match.natural-end", arenaId));

                        if (t.pos1 != null) {
                            int removed = 0;
                            for (var boat : t.pos1.getWorld().getEntitiesByClass(org.bukkit.entity.Boat.class)) {
                                if (boat.getLocation().distance(t.pos1) <= 100) {
                                    boat.remove();
                                    removed++;
                                }
                            }
                            plugin.broadcastRadius(arenaId, plugin.getMessage("boat.clear-complete", arenaId,
                                    "{count}", String.valueOf(removed)));
                        }

                        t.matchTimer = null;
                        t.currentPeriod = 0;
                    }

                    ended = true;
                    running = false;
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(WTime.get(), 0L, 20L);
    }

    private void updateBossBar() {
        WTime plugin = WTime.get();
        TimerInstance t = plugin.timers[arenaId];
        if (t.breakBossBar == null) return;

        String time = getFormattedTime();
        String titleText;

        if (remainingSeconds <= 5 && remainingSeconds > 0) {
            isRed = !isRed;
            if (isRed) {
                titleText = "&#FF5555&lПЕРЕРЫВ &f" + time;
                t.breakBossBar.setColor(BarColor.RED);
            } else {
                titleText = "&#55AAFF&lПЕРЕРЫВ &f" + time;
                t.breakBossBar.setColor(BarColor.BLUE);
            }
        } else {
            titleText = "&#55AAFF&lПЕРЕРЫВ &f" + time;
            t.breakBossBar.setColor(BarColor.BLUE);
        }

        t.breakBossBar.setTitle(plugin.convertHexToLegacy(titleText));
        t.breakBossBar.setProgress(Math.max(0, Math.min(1, (double) remainingSeconds / totalSeconds)));

        if (t.pos2 != null) {
            for (var player : t.pos2.getWorld().getPlayers()) {
                if (player.getLocation().distance(t.pos2) <= 100) {
                    if (!t.breakBossBar.getPlayers().contains(player)) {
                        t.breakBossBar.addPlayer(player);
                    }
                }
            }
        }
    }

    public void stop() {
        running = false;
        ended = true;
        if (isBreak) {
            WTime plugin = WTime.get();
            TimerInstance t = plugin.timers[arenaId];
            if (t.breakBossBar != null) {
                t.breakBossBar.removeAll();
                t.breakBossBar = null;
            }
        }
    }

    public boolean isRunning() {
        return running && !ended;
    }

    public boolean isFinished() {
        return remainingSeconds <= 0 || ended;
    }

    public String getFormattedTime() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}