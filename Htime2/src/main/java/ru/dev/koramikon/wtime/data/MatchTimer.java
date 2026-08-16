package ru.dev.koramikon.wtime.data;

import org.bukkit.scheduler.BukkitRunnable;
import ru.dev.koramikon.wtime.WTime;

public class MatchTimer {
    private int remainingSeconds;
    private boolean running = false;
    private boolean isBreak;
    private boolean ended = false;
    private boolean firstTick = true; // Флаг первого тика

    public MatchTimer(int seconds, boolean isBreak) {
        this.remainingSeconds = seconds;
        this.isBreak = isBreak;
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

                // Если это первый тик - пропускаем уменьшение
                if (firstTick) {
                    firstTick = false;
                    return;
                }

                // Проверяем окончание
                if (remainingSeconds <= 0) {
                    ended = true;
                    running = false;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            WTime.get().matchNaturallyEnded();
                        }
                    }.runTask(WTime.get());

                    cancel();
                    return;
                }

                // Уменьшаем время
                if (remainingSeconds > 0) {
                    if (isBreak) {
                        remainingSeconds--;
                    } else {
                        if (WTime.get().timersRunning) {
                            remainingSeconds--;
                        }
                    }
                }
            }
        }.runTaskTimer(WTime.get(), 0L, 20L);
    }

    public void stop() {
        running = false;
        ended = true;
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