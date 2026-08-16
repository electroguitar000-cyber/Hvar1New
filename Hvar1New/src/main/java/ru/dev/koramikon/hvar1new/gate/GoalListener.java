package ru.dev.koramikon.hvar1new.gate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import ru.dev.koramikon.hvar1new.Hvar1NewPlugin;

import java.util.*;

public class GoalListener implements Listener {

    private final Hvar1NewPlugin plugin;
    private final Map<UUID, BukkitTask> pendingGoals = new HashMap<>();
    private final Map<UUID, Long> lastGoalTime = new HashMap<>();
    private final Map<UUID, String> lastGoalSide = new HashMap<>();
    private final Map<UUID, SaveEntry> saveEntries = new HashMap<>();

    // Флаги входа/выхода для створов
    private final Map<UUID, Map<String, Boolean>> stvorStatesMap = new HashMap<>();
    // Таймеры выхода (задержка 2 секунды)
    private final Map<UUID, Map<String, BukkitTask>> stvorExitTimers = new HashMap<>();

    private static final double BOAT_VOLUME = 1.375 * 0.5625 * 1.375;
    private static final double REQUIRED_VOLUME = (20.5 / 22.0) * BOAT_VOLUME;
    private static final long STVOR_EXIT_DELAY = 40L; // 2 секунды (40 тиков)

    public GoalListener(Hvar1NewPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat boat)) return;

        UUID boatId = boat.getUniqueId();
        BoundingBox boatBB = boat.getBoundingBox();
        int foundArena = -1;
        String side = null;
        double maxVolume = 0.0;

        // ---------- 1. Проверка ворот (гол) ----------
        for (int i = 1; i <= 50; i++) {
            if (!plugin.gatesEnabledArr[i]) continue;
            BoundingBox left = plugin.leftGates[i].getGoalBB();
            if (left != null && boatBB.overlaps(left)) {
                double vol = Math.max(0, boatBB.clone().intersection(left).getVolume());
                if (vol > maxVolume) { maxVolume = vol; foundArena = i; side = "L"; }
            }
            BoundingBox right = plugin.rightGates[i].getGoalBB();
            if (right != null && boatBB.overlaps(right)) {
                double vol = Math.max(0, boatBB.clone().intersection(right).getVolume());
                if (vol > maxVolume) { maxVolume = vol; foundArena = i; side = "R"; }
            }
        }

        // ---------- 2. Гол ----------
        if (foundArena != -1 && maxVolume >= REQUIRED_VOLUME) {
            long now = System.currentTimeMillis();
            if (!pendingGoals.containsKey(boatId)) {
                if (now - lastGoalTime.getOrDefault(boatId, 0L) < 5000L) return;
                long delay = plugin.gateDelayTicks[foundArena];
                final int finalArena = foundArena;
                final String finalSide = side;

                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    pendingGoals.remove(boatId);
                    if (!boat.isValid()) return;
                    BoundingBox curBB = boat.getBoundingBox();
                    double curVol = 0.0;
                    String curSide = null;
                    BoundingBox lBB = plugin.leftGates[finalArena].getGoalBB();
                    if (lBB != null && curBB.overlaps(lBB)) {
                        double v = Math.max(0, curBB.clone().intersection(lBB).getVolume());
                        if (v > curVol) { curVol = v; curSide = "L"; }
                    }
                    BoundingBox rBB = plugin.rightGates[finalArena].getGoalBB();
                    if (rBB != null && curBB.overlaps(rBB)) {
                        double v = Math.max(0, curBB.clone().intersection(rBB).getVolume());
                        if (v > curVol) { curVol = v; curSide = "R"; }
                    }
                    if (curVol >= REQUIRED_VOLUME && curSide != null) {
                        long goalTime = System.currentTimeMillis();
                        lastGoalTime.put(boatId, goalTime);
                        lastGoalSide.put(boatId, curSide);
                        plugin.handleGoal(finalArena, boat, curSide);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (boat.isValid()) boat.remove();
                        }, 20L);
                    }
                }, delay);
                pendingGoals.put(boatId, task);
            }
        }

        // ---------- 3. Створы с задержкой 2 секунды ----------
        Map<String, Boolean> stvorStates = stvorStatesMap.computeIfAbsent(boatId, k -> new HashMap<>());
        Map<String, BukkitTask> exitTimers = stvorExitTimers.computeIfAbsent(boatId, k -> new HashMap<>());

        for (int i = 1; i <= 50; i++) {
            if (!plugin.gatesEnabledArr[i]) continue;
            checkStvorWithCooldown(boatId, i, 1, boatBB, stvorStates, exitTimers, "L");
            checkStvorWithCooldown(boatId, i, 3, boatBB, stvorStates, exitTimers, "L");
            checkStvorWithCooldown(boatId, i, 2, boatBB, stvorStates, exitTimers, "R");
            checkStvorWithCooldown(boatId, i, 4, boatBB, stvorStates, exitTimers, "R");
        }

        // ---------- 4. Сейвы ----------
        boolean inSaveL = isInSaveZone(boatBB, plugin.saveL, foundArena != -1 ? foundArena : 0);
        boolean inSaveR = isInSaveZone(boatBB, plugin.saveR, foundArena != -1 ? foundArena : 0);

        String saveSide = null;
        int saveArena = -1;
        if (inSaveL) {
            saveSide = "L";
            saveArena = (foundArena != -1) ? foundArena : findArenaBySave(boatBB, true);
        } else if (inSaveR) {
            saveSide = "R";
            saveArena = (foundArena != -1) ? foundArena : findArenaBySave(boatBB, false);
        }

        if (saveSide != null && saveArena != -1) {
            SaveEntry entry = saveEntries.get(boatId);
            if (entry == null) {
                saveEntries.put(boatId, new SaveEntry(saveSide, saveArena, System.currentTimeMillis()));
            } else {
                if (!entry.side.equals(saveSide) || entry.arena != saveArena) {
                    processSaveExit(boatId, entry);
                    saveEntries.put(boatId, new SaveEntry(saveSide, saveArena, System.currentTimeMillis()));
                } else {
                    entry.entryTime = System.currentTimeMillis();
                }
            }
        } else {
            SaveEntry entry = saveEntries.remove(boatId);
            if (entry != null) {
                processSaveExit(boatId, entry);
            }
        }

        // ---------- 5. Отмена гола ----------
        BukkitTask task = pendingGoals.get(boatId);
        if (task != null && foundArena == -1) {
            task.cancel();
            pendingGoals.remove(boatId);
        }
    }

    private void checkStvorWithCooldown(UUID boatId, int arenaId, int stvorNum, BoundingBox boatBox,
                                        Map<String, Boolean> states, Map<String, BukkitTask> exitTimers, String side) {
        GoalSide stvor = getStvor(arenaId, stvorNum);
        if (stvor == null) return;
        BoundingBox bb = getStvorBB(stvor);
        if (bb == null) return;

        String key = arenaId + "|" + stvorNum;
        boolean overlapping = boatBox.overlaps(bb);
        boolean previously = states.getOrDefault(key, false);

        // Отменяем существующий таймер выхода при входе
        BukkitTask exitTask = exitTimers.remove(key);
        if (exitTask != null) {
            exitTask.cancel();
        }

        if (overlapping) {
            if (!previously) {
                if (exitTask == null) {
                    // Засчитываем створ (только если мы вышли больше 2 сек назад)
                    if (plugin.gatesEnabledArr[arenaId]) {
                        plugin.statsManagers[arenaId].addStvor(side);
                    }
                    states.put(key, true);
                } else {
                    // Зашли раньше 2 секунд – створ НЕ засчитываем
                    states.put(key, true);
                }
            }
        } else {
            if (previously) {
                // Выход – запускаем таймер на 2 секунды
                BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    states.put(key, false);
                    exitTimers.remove(key);
                }, STVOR_EXIT_DELAY);
                exitTimers.put(key, task);
            }
        }
    }

    private GoalSide getStvor(int arenaId, int num) {
        switch (num) {
            case 1: return plugin.stvor1[arenaId];
            case 2: return plugin.stvor2[arenaId];
            case 3: return plugin.stvor3[arenaId];
            case 4: return plugin.stvor4[arenaId];
            default: return null;
        }
    }

    private BoundingBox getStvorBB(GoalSide stvor) {
        if (stvor == null) return null;
        if (stvor.hasBoth()) {
            return stvor.getGoalBB();
        }
        Location loc = stvor.getPos1();
        if (loc == null) return null;
        return new BoundingBox(loc.getX() - 0.5, loc.getY(), loc.getZ() - 0.5,
                loc.getX() + 0.5, loc.getY() + 3.0, loc.getZ() + 0.5);
    }

    // ----- Методы для сейвов -----
    private boolean isInSaveZone(BoundingBox boatBox, GoalSide[] saves, int arenaId) {
        if (arenaId > 0 && saves[arenaId] != null && saves[arenaId].getGoalBB() != null) {
            return boatBox.overlaps(saves[arenaId].getGoalBB());
        }
        for (int i = 1; i <= 50; i++) {
            if (saves[i] != null && saves[i].getGoalBB() != null && boatBox.overlaps(saves[i].getGoalBB())) {
                return true;
            }
        }
        return false;
    }

    private int findArenaBySave(BoundingBox boatBox, boolean isLeft) {
        GoalSide[] saves = isLeft ? plugin.saveL : plugin.saveR;
        for (int i = 1; i <= 50; i++) {
            if (saves[i] != null && saves[i].getGoalBB() != null && boatBox.overlaps(saves[i].getGoalBB())) {
                return i;
            }
        }
        return -1;
    }

    private void processSaveExit(UUID boatId, SaveEntry entry) {
        Long goalTime = lastGoalTime.get(boatId);
        String goalSide = lastGoalSide.get(boatId);
        if (goalTime != null && goalSide != null && goalTime >= entry.entryTime && goalSide.equals(entry.side)) {
            return;
        }
        if (plugin.gatesEnabledArr[entry.arena]) {
            plugin.statsManagers[entry.arena].addSave(entry.side);
        }
    }

    private static class SaveEntry {
        String side;
        int arena;
        long entryTime;
        SaveEntry(String side, int arena, long entryTime) {
            this.side = side;
            this.arena = arena;
            this.entryTime = entryTime;
        }
    }
}